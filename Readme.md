## Usage

Check your v2ray configs for internet access

### Pull

pull this image from the docker registry

```bash
docker pull amirabdol/v2raychecker
```

### Run

suppose you are in current directory (pwd),
and you want to check all v2ray configs that are in
configs.txt and filter valid config into valid_configs.txt

```bash
docker --rm -v ${pwd}:/app amirabdol/v2raychecker --output=valid_configs.txt configs.txt
```

if you don't provide output file
all configs and its status will be printed
into console

