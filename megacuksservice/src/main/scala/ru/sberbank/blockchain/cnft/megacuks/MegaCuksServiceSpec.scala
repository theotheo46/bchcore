package ru.sberbank.blockchain.cnft.megacuks

import tools.http.service.annotations.{Body, HttpHeaderValue, HttpPost}

/**
 * @author Andrew Pudovikov
 */

trait MegaCuksServiceSpec[R[_]] {

    @HttpPost("/MegaCUKSServ/inputReq")
    @HttpHeaderValue("Content-type", "application/xml")
    def requestSign(@Body request: SignByRequest): R[SignByResponse]

    @HttpPost("/MegaCUKSServ/inputReq")
    @HttpHeaderValue("Content-type", "application/xml")
    def requestVerify(@Body request: CheckDocSignDetachReq): R[CheckDocSignDetachRes]
}

