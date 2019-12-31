# Entrypoint Catcher
A mod to deal with mods trying  to redirect Fabric's mod initialization syatem, by providing an api for all to use

### Get it from Jitpack
```groovy
repositories {
    maven { url "https://jitpack.io" }
}
dependencies {
    modImplementation 'com.github.giantnuker:EntrypointCatcher:<version>'
    include 'com.github.giantnuker:EntrypointCatcher:<version>'
}
```
You can find the version under the releases tab.

### Now how do I use it???
If you want to log whats happening, inject at a certain point, check for errors, etc, you'll want to make an `EntrypointHandler`
To mark your handler to be used, put this in your `fabric.mod.json`:
```json
"entrypoints": {
  "entry_handler": [
    "<your entrypoint, extending EntrypointHandler>"
  ]
}
```
You can implement the following methods:

`onBegin` for a hook before modloading starts

`processContainer` to let your mod have a peek at all the ModContainers, possibly find entrypoints/flags

`onModsInstanced` to catch errors in mod initialization and for a hook after its done

`onModInitializeBegin` to have a hook before each mod is initialized

`onModInitializeEnd` to have a hook after each mod is initialized and catch any errors

`onCommonInitializerBegin` a hook for when common mod initialization starts

`onClientInitializerBegin` a hook for when client mod initialization starts

`onEnd` a hook for after all mods are done initializing

#### What if it isn't enough?
Well, theres a solution for that too, but if you find a use case for something, please make a PR. If you need to do something crazy like move the modloading completely, You'll want to redirect the handler...

Be advised, doing this **WILL BREAK OTHER MODS**. Namely *Informed Load*, as it does the same thing.
Only one redirect can be used at a time. The newest one registered will overwrite the older one.

However, if you *really* need to, you can:

1. Make a pre-launch entrypoint by adding this to your `fabric.mod.json`:
```json
"entrypoints": {
  "preLauch": [
    "<your entrypoint, extending PreLaunchEntrypoint>"
  ]
}
```
2. Calling `EntrypointCatcher.`