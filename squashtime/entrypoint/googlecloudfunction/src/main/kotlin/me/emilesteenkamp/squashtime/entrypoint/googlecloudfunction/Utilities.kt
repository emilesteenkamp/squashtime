package me.emilesteenkamp.squashtime.entrypoint.googlecloudfunction

import java.io.File
import java.net.URL

fun URL.asJavaFile(): File = File(this.file)