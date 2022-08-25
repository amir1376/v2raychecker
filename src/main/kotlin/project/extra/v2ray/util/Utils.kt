package project.extra.v2ray.util

import project.extra.v2ray.V2RayConstants
import project.utils.Base64Util
import java.net.*

object Utils {

    /**
     * parseInt
     */
    fun parseInt(str: String): Int {
        return parseInt(str, 0)
    }

    fun parseInt(str: String?, default: Int): Int {
        str ?: return default
        return try {
            Integer.parseInt(str)
        } catch (e: Exception) {
            e.printStackTrace()
            default
        }
    }

    /**
     * base64 decode
     */
    fun decode(text: String): String {
        tryDecodeBase64(text)?.let { return it }
        if (text.endsWith('=')) {
            // try again for some loosely formatted base64
            tryDecodeBase64(text.trimEnd('='))?.let { return it }
        }
        return ""
    }

    fun tryDecodeBase64(text: String): String? {
        try {
            return Base64Util.decode(text).toString(charset("UTF-8"))
        } catch (_: Exception) {
        }
        try {
            return Base64Util.decode(text)
                .toString(charset("UTF-8"))
        } catch (_: Exception) {
        }
        return null
    }

    /**
     * base64 encode
     */
    fun encode(text: String): String {
        return try {
            Base64Util.encode(text.toByteArray(charset("UTF-8")))
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }


    fun getVpnDnsServers(): List<String> {
        val vpnDns = V2RayConstants.DNS_AGENT
        return vpnDns.split(",").filter { isPureIpAddress(it) }
        // allow empty, in that case dns will use system default
    }

    /**
     * is ip address
     */
    fun isIpAddress(value: String): Boolean {
        try {
            var addr = value
            if (addr.isEmpty() || addr.isBlank()) {
                return false
            }
            //CIDR
            if (addr.indexOf("/") > 0) {
                val arr = addr.split("/")
                if (arr.count() == 2 && Integer.parseInt(arr[1]) > 0) {
                    addr = arr[0]
                }
            }

            // "::ffff:192.168.173.22"
            // "[::ffff:192.168.173.22]:80"
            if (addr.startsWith("::ffff:") && '.' in addr) {
                addr = addr.drop(7)
            } else if (addr.startsWith("[::ffff:") && '.' in addr) {
                addr = addr.drop(8).replace("]", "")
            }

            // addr = addr.toLowerCase()
            val octets = addr.split('.').toTypedArray()
            if (octets.size == 4) {
                if (octets[3].indexOf(":") > 0) {
                    addr = addr.substring(0, addr.indexOf(":"))
                }
                return isIpv4Address(addr)
            }

            // Ipv6addr [2001:abc::123]:8080
            return isIpv6Address(addr)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun isPureIpAddress(value: String): Boolean {
        return (isIpv4Address(value) || isIpv6Address(value))
    }

    fun isIpv4Address(value: String): Boolean {
        val regV4 =
            Regex("^([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])$")
        return regV4.matches(value)
    }

    fun isIpv6Address(value: String): Boolean {
        var addr = value
        if (addr.indexOf("[") == 0 && addr.lastIndexOf("]") > 0) {
            addr = addr.drop(1)
            addr = addr.dropLast(addr.count() - addr.lastIndexOf("]"))
        }
        val regV6 =
            Regex("^((?:[0-9A-Fa-f]{1,4}))?((?::[0-9A-Fa-f]{1,4}))*::((?:[0-9A-Fa-f]{1,4}))?((?::[0-9A-Fa-f]{1,4}))*|((?:[0-9A-Fa-f]{1,4}))((?::[0-9A-Fa-f]{1,4})){7}$")
        return regV6.matches(addr)
    }

    private fun isCoreDNSAddress(s: String): Boolean {
        return s.startsWith("https") || s.startsWith("tcp") || s.startsWith("quic")
    }


    fun urlDecode(url: String): String {
        return try {
            URLDecoder.decode(URLDecoder.decode(url), "utf-8")
        } catch (e: Exception) {
            e.printStackTrace()
            url
        }
    }

    fun urlEncode(url: String): String {
        return try {
            URLEncoder.encode(url, "UTF-8")
        } catch (e: Exception) {
            e.printStackTrace()
            url
        }
    }


    fun getIpv6Address(address: String): String {
        return if (isIpv6Address(address)) {
            String.format("[%s]", address)
        } else {
            address
        }
    }

    fun fixIllegalUrl(str: String): String {
        return str
            .replace(" ", "%20")
            .replace("|", "%7C")
    }
}

