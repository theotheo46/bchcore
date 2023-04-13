package ru.sberbank.blockchain.cnft.gate.filter

import jakarta.servlet.http.{HttpServletRequest, HttpServletRequestWrapper}

import java.io.{BufferedReader, ByteArrayInputStream, InputStreamReader}
import java.nio.charset.StandardCharsets

class CachedBodyHttpServletRequestWrapper(
    request: HttpServletRequest
) extends HttpServletRequestWrapper(request) {

    private[filter] val cachedBody =
        try getInputStream.readAllBytes()
        finally getInputStream.close()

    override def getReader: BufferedReader =
        new BufferedReader(
            new InputStreamReader(
                new ByteArrayInputStream(cachedBody),
                StandardCharsets.UTF_8
            )
        )

}