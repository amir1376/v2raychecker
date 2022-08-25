package project.extra.v2ray.util

import com.google.gson.Gson
import project.extra.v2ray.dto.V2rayConfig.Companion.DEFAULT_SECURITY
import project.extra.v2ray.dto.V2rayConfig.Companion.TLS
import java.net.URI
import project.extra.v2ray.extension.idnHost
import project.extra.v2ray.dto.EConfigType
import project.extra.v2ray.dto.ServerConfig
import project.extra.v2ray.dto.V2rayConfig
import project.extra.v2ray.dto.VmessQRCode
import java.util.HashMap

object AngConfigManager {
    fun shareConfig(config: ServerConfig): String {
        try {
            val outbound = config.getProxyOutbound() ?: return ""
            val streamSetting = outbound.streamSettings ?: return ""
            return config.configType.protocolScheme + when (config.configType) {
                EConfigType.VMESS -> {
                    val vmessQRCode = VmessQRCode()
                    vmessQRCode.v = "2"
                    vmessQRCode.ps = config.remarks
                    vmessQRCode.add = outbound.getServerAddress().orEmpty()
                    vmessQRCode.port = outbound.getServerPort().toString()
                    vmessQRCode.id = outbound.getPassword().orEmpty()
                    vmessQRCode.aid = outbound.settings?.vnext?.get(0)?.users?.get(0)?.alterId.toString()
                    vmessQRCode.scy = outbound.settings?.vnext?.get(0)?.users?.get(0)?.security.toString()
                    vmessQRCode.net = streamSetting.network
                    vmessQRCode.tls = streamSetting.security
                    vmessQRCode.sni = streamSetting.tlsSettings?.serverName.orEmpty()
                    outbound.getTransportSettingDetails()?.let { transportDetails ->
                        vmessQRCode.type = transportDetails[0]
                        vmessQRCode.host = transportDetails[1]
                        vmessQRCode.path = transportDetails[2]
                    }
                    val json = Gson().toJson(vmessQRCode)
                    Utils.encode(json)
                }

                EConfigType.CUSTOM -> ""
                EConfigType.SHADOWSOCKS -> {
                    val remark = "#" + Utils.urlEncode(config.remarks)
                    val pw = Utils.encode("${outbound.getSecurityEncryption()}:${outbound.getPassword()}")
                    val url = String.format(
                        "%s@%s:%s",
                        pw,
                        Utils.getIpv6Address(outbound.getServerAddress()!!),
                        outbound.getServerPort()
                    )
                    url + remark
                }

                EConfigType.SOCKS -> {
                    val remark = "#" + Utils.urlEncode(config.remarks)
                    val pw =
                        Utils.encode("${outbound.settings?.servers?.get(0)?.users?.get(0)?.user}:${outbound.getPassword()}")
                    val url = String.format(
                        "%s@%s:%s",
                        pw,
                        Utils.getIpv6Address(outbound.getServerAddress()!!),
                        outbound.getServerPort()
                    )
                    url + remark
                }

                EConfigType.VLESS,
                EConfigType.TROJAN -> {
                    val remark = "#" + Utils.urlEncode(config.remarks)

                    val dicQuery = HashMap<String, String>()
                    if (config.configType == EConfigType.VLESS) {
                        outbound.settings?.vnext?.get(0)?.users?.get(0)?.flow?.let {
                            if (!TextUtils.isEmpty(it)) {
                                dicQuery["flow"] = it
                            }
                        }
                        dicQuery["encryption"] =
                            if (outbound.getSecurityEncryption().isNullOrEmpty()) "none"
                            else outbound.getSecurityEncryption().orEmpty()
                    } else if (config.configType == EConfigType.TROJAN) {
                        config.outboundBean?.settings?.servers?.get(0)?.flow?.let {
                            if (!TextUtils.isEmpty(it)) {
                                dicQuery["flow"] = it
                            }
                        }
                    }

                    dicQuery["security"] = streamSetting.security.ifEmpty { "none" }
                    (streamSetting.tlsSettings ?: streamSetting.xtlsSettings)?.let { tlsSetting ->
                        if (!TextUtils.isEmpty(tlsSetting.serverName)) {
                            dicQuery["sni"] = tlsSetting.serverName
                        }
                    }
                    dicQuery["type"] = streamSetting.network.ifEmpty { V2rayConfig.DEFAULT_NETWORK }

                    outbound.getTransportSettingDetails()?.let { transportDetails ->
                        when (streamSetting.network) {
                            "tcp" -> {
                                dicQuery["headerType"] = transportDetails[0].ifEmpty { "none" }
                                if (!TextUtils.isEmpty(transportDetails[1])) {
                                    dicQuery["host"] = Utils.urlEncode(transportDetails[1])
                                }
                            }

                            "kcp" -> {
                                dicQuery["headerType"] = transportDetails[0].ifEmpty { "none" }
                                if (!TextUtils.isEmpty(transportDetails[2])) {
                                    dicQuery["seed"] = Utils.urlEncode(transportDetails[2])
                                }
                            }

                            "ws" -> {
                                if (!TextUtils.isEmpty(transportDetails[1])) {
                                    dicQuery["host"] = Utils.urlEncode(transportDetails[1])
                                }
                                if (!TextUtils.isEmpty(transportDetails[2])) {
                                    dicQuery["path"] = Utils.urlEncode(transportDetails[2])
                                }
                            }

                            "http", "h2" -> {
                                dicQuery["type"] = "http"
                                if (!TextUtils.isEmpty(transportDetails[1])) {
                                    dicQuery["host"] = Utils.urlEncode(transportDetails[1])
                                }
                                if (!TextUtils.isEmpty(transportDetails[2])) {
                                    dicQuery["path"] = Utils.urlEncode(transportDetails[2])
                                }
                            }

                            "quic" -> {
                                dicQuery["headerType"] = transportDetails[0].ifEmpty { "none" }
                                dicQuery["quicSecurity"] = Utils.urlEncode(transportDetails[1])
                                dicQuery["key"] = Utils.urlEncode(transportDetails[2])
                            }

                            "grpc" -> {
                                dicQuery["mode"] = transportDetails[0]
                                dicQuery["serviceName"] = transportDetails[2]
                            }
                        }
                    }
                    val query = "?" + dicQuery.toList().joinToString(
                        separator = "&",
                        transform = { it.first + "=" + it.second })

                    val url = String.format(
                        "%s@%s:%s",
                        outbound.getPassword(),
                        Utils.getIpv6Address(outbound.getServerAddress()!!),
                        outbound.getServerPort()
                    )
                    url + query + remark
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    fun getConfigFromString(
        str: String,
    ): ServerConfig {
        try {
            var config: ServerConfig? = null
            val allowInsecure = true
            when {
                str.startsWith(EConfigType.VMESS.protocolScheme) -> {
                    config = ServerConfig.create(EConfigType.VMESS)
                    val streamSetting = config.outboundBean?.streamSettings
                        ?: throw Exception("streamSettings stream settings is null!")

                    if (!tryParseNewVmess(str, config, allowInsecure)) {
                        if (str.indexOf("?") > 0) {
                            if (!tryResolveVmess4Kitsunebi(str, config)) {
                                incorrectProtocol()
                            }
                        } else {
                            var result = str.replace(EConfigType.VMESS.protocolScheme, "")
                            result = Utils.decode(result)
                            if (TextUtils.isEmpty(result)) {
                                decondingFailed()
                            }
                            val vmessQRCode = Gson().fromJson(result, VmessQRCode::class.java)
                            // Although VmessQRCode fields are non null, looks like Gson may still create null fields
                            if (TextUtils.isEmpty(vmessQRCode.add)
                                || TextUtils.isEmpty(vmessQRCode.port)
                                || TextUtils.isEmpty(vmessQRCode.id)
                                || TextUtils.isEmpty(vmessQRCode.net)
                            ) {
                                incorrectProtocol()
                            }

                            config.remarks = vmessQRCode.ps
                            config.outboundBean?.settings?.vnext?.get(0)?.let { vnext ->
                                vnext.address = vmessQRCode.add
                                vnext.port = Utils.parseInt(vmessQRCode.port)
                                vnext.users[0].id = vmessQRCode.id
                                vnext.users[0].security =
                                    if (TextUtils.isEmpty(vmessQRCode.scy)) DEFAULT_SECURITY else vmessQRCode.scy
                                vnext.users[0].alterId = Utils.parseInt(vmessQRCode.aid)
                            }
                            val sni = streamSetting.populateTransportSettings(
                                vmessQRCode.net,
                                vmessQRCode.type,
                                vmessQRCode.host,
                                vmessQRCode.path,
                                vmessQRCode.path,
                                vmessQRCode.host,
                                vmessQRCode.path,
                                vmessQRCode.type,
                                vmessQRCode.path
                            )
                            streamSetting.populateTlsSettings(
                                vmessQRCode.tls, allowInsecure,
                                if (TextUtils.isEmpty(vmessQRCode.sni)) sni else vmessQRCode.sni
                            )
                        }
                    }
                }

                str.startsWith(EConfigType.SHADOWSOCKS.protocolScheme) -> {
                    config = ServerConfig.create(EConfigType.SHADOWSOCKS)
                    if (!tryResolveResolveSip002(str, config)) {
                        var result = str.replace(EConfigType.SHADOWSOCKS.protocolScheme, "")
                        val indexSplit = result.indexOf("#")
                        if (indexSplit > 0) {
                            try {
                                config.remarks =
                                    Utils.urlDecode(result.substring(indexSplit + 1, result.length))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }

                            result = result.substring(0, indexSplit)
                        }

                        //part decode
                        val indexS = result.indexOf("@")
                        result = if (indexS > 0) {
                            Utils.decode(result.substring(0, indexS)) + result.substring(
                                indexS,
                                result.length
                            )
                        } else {
                            Utils.decode(result)
                        }

                        val legacyPattern = "^(.+?):(.*)@(.+?):(\\d+?)/?$".toRegex()
                        val match = legacyPattern.matchEntire(result)
                            ?: incorrectProtocol()

                        config.outboundBean?.settings?.servers?.get(0)?.let { server ->
                            server.address = match.groupValues[3].removeSurrounding("[", "]")
                            server.port = match.groupValues[4].toInt()
                            server.password = match.groupValues[2]
                            server.method = match.groupValues[1].lowercase()
                        }
                    }
                }

                str.startsWith(EConfigType.SOCKS.protocolScheme) -> {
                    var result = str.replace(EConfigType.SOCKS.protocolScheme, "")
                    val indexSplit = result.indexOf("#")
                    config = ServerConfig.create(EConfigType.SOCKS)
                    if (indexSplit > 0) {
                        try {
                            config.remarks =
                                Utils.urlDecode(result.substring(indexSplit + 1, result.length))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        result = result.substring(0, indexSplit)
                    }

                    //part decode
                    val indexS = result.indexOf("@")
                    if (indexS > 0) {
                        result = Utils.decode(result.substring(0, indexS)) + result.substring(
                            indexS,
                            result.length
                        )
                    } else {
                        result = Utils.decode(result)
                    }

                    val legacyPattern = "^(.*):(.*)@(.+?):(\\d+?)$".toRegex()
                    val match =
                        legacyPattern.matchEntire(result) ?: incorrectProtocol()

                    config.outboundBean?.settings?.servers?.get(0)?.let { server ->
                        server.address = match.groupValues[3].removeSurrounding("[", "]")
                        server.port = match.groupValues[4].toInt()
                        val socksUsersBean =
                            V2rayConfig.OutboundBean.OutSettingsBean.ServersBean.SocksUsersBean()
                        socksUsersBean.user = match.groupValues[1].lowercase()
                        socksUsersBean.pass = match.groupValues[2]
                        server.users = listOf(socksUsersBean)
                    }
                }

                str.startsWith(EConfigType.TROJAN.protocolScheme) -> {
                    val uri = URI(Utils.fixIllegalUrl(str))
                    config = ServerConfig.create(EConfigType.TROJAN)
                    config.remarks = Utils.urlDecode(uri.fragment ?: "")

                    var flow = ""
                    if (uri.rawQuery != null) {
                        val queryParam = uri.rawQuery.split("&")
                            .associate { it.split("=").let { (k, v) -> k to Utils.urlDecode(v) } }

                        val sni = config.outboundBean?.streamSettings?.populateTransportSettings(
                            queryParam["type"] ?: "tcp",
                            queryParam["headerType"],
                            queryParam["host"],
                            queryParam["path"],
                            queryParam["seed"],
                            queryParam["quicSecurity"],
                            queryParam["key"],
                            queryParam["mode"],
                            queryParam["serviceName"]
                        )
                        config.outboundBean?.streamSettings?.populateTlsSettings(
                            queryParam["security"] ?: TLS, allowInsecure, queryParam["sni"] ?: sni!!
                        )
                        flow = queryParam["flow"] ?: ""
                    } else {
                        config.outboundBean?.streamSettings?.populateTlsSettings(
                            TLS,
                            allowInsecure,
                            ""
                        )
                    }

                    config.outboundBean?.settings?.servers?.get(0)?.let { server ->
                        server.address = uri.idnHost
                        server.port = uri.port
                        server.password = uri.userInfo
                        server.flow = flow
                    }
                }

                str.startsWith(EConfigType.VLESS.protocolScheme) -> {
                    val uri = URI(Utils.fixIllegalUrl(str))
                    val queryParam = uri.rawQuery.split("&")
                        .associate { it.split("=").let { (k, v) -> k to Utils.urlDecode(v) } }
                    config = ServerConfig.create(EConfigType.VLESS)
                    val streamSetting =
                        config.outboundBean?.streamSettings
                            ?: throw Exception("streamSettings is null")
                    config.remarks = Utils.urlDecode(uri.fragment ?: "")
                    config.outboundBean?.settings?.vnext?.get(0)?.let { vnext ->
                        vnext.address = uri.idnHost
                        vnext.port = uri.port
                        vnext.users[0].id = uri.userInfo
                        vnext.users[0].encryption = queryParam["encryption"] ?: "none"
                        vnext.users[0].flow = queryParam["flow"] ?: ""
                    }

                    val sni = streamSetting.populateTransportSettings(
                        queryParam["type"] ?: "tcp",
                        queryParam["headerType"],
                        queryParam["host"],
                        queryParam["path"],
                        queryParam["seed"],
                        queryParam["quicSecurity"],
                        queryParam["key"],
                        queryParam["mode"],
                        queryParam["serviceName"]
                    )
                    streamSetting.populateTlsSettings(
                        queryParam["security"] ?: "",
                        allowInsecure,
                        queryParam["sni"] ?: sni
                    )
                }
            }
            if (config == null) {
                incorrectProtocol()
            }
            return config
        } catch (e: Exception) {
            //we cant handle this string
            throw e
        }
    }

    private fun incorrectProtocol(): Nothing {
        throw Exception("Incorrect Protocol")
    }

    private fun decondingFailed(): Nothing {
        throw Exception("decoding failed")
    }

    private fun tryParseNewVmess(
        uriString: String,
        config: ServerConfig,
        allowInsecure: Boolean
    ): Boolean {
        return runCatching {
            val uri = URI(uriString)
            check(uri.scheme == "vmess")
            val (_, protocol, tlsStr, uuid, alterId) =
                Regex("(tcp|http|ws|kcp|quic|grpc)(\\+tls)?:([0-9a-z]{8}-[0-9a-z]{4}-[0-9a-z]{4}-[0-9a-z]{4}-[0-9a-z]{12})")
                    .matchEntire(uri.userInfo)?.groupValues
                    ?: error("parse user info fail.")
            val tls = tlsStr.isNotBlank()
            val queryParam = uri.rawQuery.split("&")
                .associate { it.split("=").let { (k, v) -> k to Utils.urlDecode(v) } }

            val streamSetting = config.outboundBean?.streamSettings ?: return false
            config.remarks = Utils.urlDecode(uri.fragment ?: "")
            config.outboundBean.settings?.vnext?.get(0)?.let { vnext ->
                vnext.address = uri.idnHost
                vnext.port = uri.port
                vnext.users[0].id = uuid
                vnext.users[0].security = DEFAULT_SECURITY
                vnext.users[0].alterId = alterId.toInt()
            }

            val sni = streamSetting.populateTransportSettings(protocol,
                queryParam["type"],
                queryParam["host"]?.split("|")?.get(0) ?: "",
                queryParam["path"]?.takeIf { it.trim() != "/" } ?: "",
                queryParam["seed"],
                queryParam["security"],
                queryParam["key"],
                queryParam["mode"],
                queryParam["serviceName"])
            streamSetting.populateTlsSettings(if (tls) TLS else "", allowInsecure, sni)
            true
        }.getOrElse { false }
    }

    private fun tryResolveVmess4Kitsunebi(server: String, config: ServerConfig): Boolean {

        var result = server.replace(EConfigType.VMESS.protocolScheme, "")
        val indexSplit = result.indexOf("?")
        if (indexSplit > 0) {
            result = result.substring(0, indexSplit)
        }
        result = Utils.decode(result)

        val arr1 = result.split('@')
        if (arr1.count() != 2) {
            return false
        }
        val arr21 = arr1[0].split(':')
        val arr22 = arr1[1].split(':')
        if (arr21.count() != 2) {
            return false
        }

        config.remarks = "Alien"
        config.outboundBean?.settings?.vnext?.get(0)?.let { vnext ->
            vnext.address = arr22[0]
            vnext.port = Utils.parseInt(arr22[1])
            vnext.users[0].id = arr21[1]
            vnext.users[0].security = arr21[0]
            vnext.users[0].alterId = 0
        }
        return true
    }

    private fun tryResolveResolveSip002(str: String, config: ServerConfig): Boolean {
        try {
            val uri = URI(Utils.fixIllegalUrl(str))
            config.remarks = Utils.urlDecode(uri.fragment ?: "")

            val method: String
            val password: String
            if (uri.userInfo.contains(":")) {
                val arrUserInfo = uri.userInfo.split(":").map { it.trim() }
                if (arrUserInfo.count() != 2) {
                    return false
                }
                method = arrUserInfo[0]
                password = Utils.urlDecode(arrUserInfo[1])
            } else {
                val base64Decode = Utils.decode(uri.userInfo)
                val arrUserInfo = base64Decode.split(":").map { it.trim() }
                if (arrUserInfo.count() < 2) {
                    return false
                }
                method = arrUserInfo[0]
                password = base64Decode.substringAfter(":")
            }

            config.outboundBean?.settings?.servers?.get(0)?.let { server ->
                server.address = uri.idnHost
                server.port = uri.port
                server.password = password
                server.method = method
            }
            return true
        } catch (e: Exception) {
            return false
        }
    }

}
