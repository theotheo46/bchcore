package ru.sberbank.blockchain.cnft.megacuks

import ru.sberbank.blockchain.cnft.commons.Result
import tools.http.service.HttpService._

/**
 * @author Andrew Pudovikov
 */
object MegaCuksService {

    def newMegaCuksService(url: String): MegaCuksServiceSpec[Result] = {
        createService[MegaCuksServiceSpec[Result]](url)
    }
}
