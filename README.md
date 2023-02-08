# Playground
### Minecraft server plugin which aims for anonymity
### Supported Minecraft version: `1.19.3`

Inspired by [noonmaru/aimless](https://github.com/noonmaru/aimless) ([monun](https://github.com/monun))

## Configuration
### Because of this project having been created for fun, it doesn't have many configurations.

`config.yml` file structure

* Spawn Seed: The random seed for the player's initial spawn location.
* skin
  * textures: The encoded base64 string which represents the player's skin info
  * signature: The skin's signature.

Generate skin textures/signature value at [Mineskin](https://mineskin.org/) 