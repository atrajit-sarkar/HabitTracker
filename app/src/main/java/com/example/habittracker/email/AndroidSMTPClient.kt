package it.atraj.habittracker.email

import android.util.Base64
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * Android-compatible SMTP client that works with Android's SSL implementation.
 * Uses raw socket communication instead of JavaMail.
 */
class AndroidSMTPClient(
    private val host: String,
    private val port: Int,
    private val username: String,
    private val password: String
) {
    private var socket: Socket? = null
    private var reader: BufferedReader? = null
    private var writer: PrintWriter? = null

    fun sendEmail(
        from: String,
        fromName: String,
        to: String,
        subject: String,
        htmlBody: String,
        textBody: String
    ) {
        try {
            Log.d(TAG, "Connecting to $host:$port")
            connect()
            
            Log.d(TAG, "Authenticating...")
            authenticate()
            
            Log.d(TAG, "Sending email...")
            sendMessage(from, fromName, to, subject, htmlBody, textBody)
            
            Log.d(TAG, "Email sent successfully!")
        } finally {
            disconnect()
        }
    }

    private fun connect() {
        // Connect with STARTTLS
        Log.d(TAG, "Creating initial connection...")
        socket = Socket(host, port)
        reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
        writer = PrintWriter(OutputStreamWriter(socket!!.getOutputStream()), true)
        
        // Read greeting
        val greeting = readResponse()
        Log.d(TAG, "Server greeting: $greeting")
        
        // Send EHLO
        sendCommand("EHLO localhost")
        val ehloResponse = readMultilineResponse()
        Log.d(TAG, "EHLO response: $ehloResponse")
        
        // Start TLS
        Log.d(TAG, "Starting TLS...")
        sendCommand("STARTTLS")
        val tlsResponse = readResponse()
        Log.d(TAG, "STARTTLS response: $tlsResponse")
        
        // Upgrade to SSL
        val sslSocket = (SSLSocketFactory.getDefault() as SSLSocketFactory)
            .createSocket(socket, host, port, true) as SSLSocket
        
        socket = sslSocket
        reader = BufferedReader(InputStreamReader(sslSocket.getInputStream()))
        writer = PrintWriter(OutputStreamWriter(sslSocket.getOutputStream()), true)
        
        Log.d(TAG, "TLS established")
        
        // Send EHLO again after TLS
        sendCommand("EHLO localhost")
        val ehloResponse2 = readMultilineResponse()
        Log.d(TAG, "EHLO after TLS: $ehloResponse2")
    }

    private fun authenticate() {
        sendCommand("AUTH LOGIN")
        readResponse()
        
        // Send username (Base64 encoded)
        val encodedUsername = Base64.encodeToString(
            username.toByteArray(), 
            Base64.NO_WRAP
        )
        sendCommand(encodedUsername)
        readResponse()
        
        // Send password (Base64 encoded)
        val encodedPassword = Base64.encodeToString(
            password.toByteArray(), 
            Base64.NO_WRAP
        )
        sendCommand(encodedPassword)
        val authResponse = readResponse()
        Log.d(TAG, "Auth response: $authResponse")
    }

    private fun sendMessage(
        from: String,
        fromName: String,
        to: String,
        subject: String,
        htmlBody: String,
        textBody: String
    ) {
        // MAIL FROM
        sendCommand("MAIL FROM:<$from>")
        readResponse()
        
        // RCPT TO
        sendCommand("RCPT TO:<$to>")
        readResponse()
        
        // DATA
        sendCommand("DATA")
        readResponse()
        
        // Email headers and body
        val boundary = "----=_Part_${System.currentTimeMillis()}"
        val outputStream = socket!!.getOutputStream()
        val CRLF = "\r\n"
        
        Log.d(TAG, "Sending email headers and body...")
        
        // Build the complete message with proper CRLF
        val message = StringBuilder()
        message.append("From: $fromName <$from>$CRLF")
        message.append("To: $to$CRLF")
        message.append("Subject: $subject$CRLF")
        message.append("MIME-Version: 1.0$CRLF")
        message.append("Content-Type: multipart/alternative; boundary=\"$boundary\"$CRLF")
        message.append(CRLF)
        
        // Plain text part
        message.append("--$boundary$CRLF")
        message.append("Content-Type: text/plain; charset=UTF-8$CRLF")
        message.append("Content-Transfer-Encoding: 7bit$CRLF")
        message.append(CRLF)
        message.append(prepareBodyForSMTP(textBody, CRLF))
        message.append(CRLF)
        
        // HTML part
        message.append("--$boundary$CRLF")
        message.append("Content-Type: text/html; charset=UTF-8$CRLF")
        message.append("Content-Transfer-Encoding: 7bit$CRLF")
        message.append(CRLF)
        message.append(prepareBodyForSMTP(htmlBody, CRLF))
        message.append(CRLF)
        
        message.append("--$boundary--$CRLF")
        message.append(CRLF)
        
        // End of message (CRLF.CRLF is the proper termination)
        message.append(".$CRLF")
        
        // Write the entire message
        outputStream.write(message.toString().toByteArray(Charsets.UTF_8))
        outputStream.flush()
        
        Log.d(TAG, "Message body sent, waiting for response...")
        val dataResponse = readResponse()
        Log.d(TAG, "DATA response: $dataResponse")
    }

    /**
     * Prepare body text for SMTP transmission
     * - Normalize line endings to CRLF
     * - Apply dot stuffing (escape lines starting with '.')
     */
    private fun prepareBodyForSMTP(body: String, CRLF: String): String {
        // First, normalize all line endings to LF only
        val normalized = body.replace("\r\n", "\n").replace("\r", "\n")
        
        // Split into lines and process each line
        val lines = normalized.split("\n")
        val result = StringBuilder()
        
        for ((index, line) in lines.withIndex()) {
            // SMTP dot stuffing: lines starting with '.' must be escaped as '..'
            val processedLine = if (line.startsWith(".")) {
                ".$line"
            } else {
                line
            }
            
            result.append(processedLine)
            
            // Add CRLF except after the last line (it will be added by caller)
            if (index < lines.size - 1) {
                result.append(CRLF)
            }
        }
        
        return result.toString()
    }
    
    private fun disconnect() {
        try {
            sendCommand("QUIT")
            readResponse()
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        } finally {
            reader?.close()
            writer?.close()
            socket?.close()
        }
    }

    private fun sendCommand(command: String) {
        Log.d(TAG, ">>> $command")
        writer?.println(command)
        writer?.flush()
    }

    private fun readResponse(): String {
        val response = reader?.readLine() ?: throw Exception("No response from server")
        Log.d(TAG, "<<< $response")
        
        if (response.startsWith("4") || response.startsWith("5")) {
            throw Exception("SMTP Error: $response")
        }
        
        return response
    }

    private fun readMultilineResponse(): String {
        val sb = StringBuilder()
        var line: String?
        
        do {
            line = reader?.readLine()
            if (line != null) {
                Log.d(TAG, "<<< $line")
                sb.appendLine(line)
            }
        } while (line != null && line.length > 3 && line[3] == '-')
        
        return sb.toString()
    }

    companion object {
        private const val TAG = "AndroidSMTPClient"
    }
}

