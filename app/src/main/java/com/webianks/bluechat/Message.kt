package com.webianks.bluechat
/**
 * Message data type for showing bluetooth chat
 * @param message Contatining every message contents
 * @param time When send message or received message
 * @param type Separating send message state or received message state
 */
data class Message(val message: String, val time: Long, val type: Int)
