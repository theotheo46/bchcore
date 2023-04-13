Feature: Smart contract regulation functionality

  Scenario: Positive scenario
    Given There is a client "SBER"

    Given There is a client "Regulator"

    Given There is a client "ICOIssuer"

#    When smart contract template for "ICO" attributes are:
#      | id                      | typeId  | description                                                                                    |
#      | dfaName                 | string  | Наименование ЦФА                                                                               |
#      | dfaAlias                | string  | Уникальный код ЦФА (сокращенное название ЦФА)                                                  |
#      | series                  | string  | Серия выпуска                                                                                  |
#      | dfaRepaymentPrice       | numeric | Объём прав при погашении (Цена одного ЦФА при погашении)                                       |
#      | repaymentDate           | string  | Дата погашения                                                                                 |
#      | paymentCurrency         | string  | Средство участия (например: rub, SBC)                                                          |
#      | softcapSum              | numeric | Объем Soft Cap                                                                                 |
#      | limitsUsage             | boolean | Признак: не использовать предельные значения для Hard Cap и лота на операцию                   |
#      | hardcapSum              | numeric | Объем Hard Cap. Не передается, если limitsUsage=True                                           |
#      | maxTransactionSum       | numeric | Объем максимального лота на операцию. Не передается, если limitsUsage=True                     |
#      | dfaPurchasePrice        | numeric | Цена приобретения одного ЦФА при выпуске                                                       |
#      | priceType               | string  | Порядок определения цены                                                                       |
#      | dfaAllocation           | string  | Распределение ЦФА                                                                              |
#      | subscriptionStartDate   | string  | Дата начала подписки                                                                           |
#      | subscriptionEndDate     | string  | Дата окночания подписки                                                                        |
#      | dfaAllocationDate       | string  | Итоговое распределение ЦФА                                                                     |
#      | projectCover            | string  | Изображение: Обложка проекта                                                                   |
#      | projectImage            | string  | Изображение: Логотип проекта                                                                   |
#      | companyWebsite          | string  | Веб-сайт компании                                                                              |
#      | shortDescription        | string  | Краткое описание проекта                                                                       |
#      | fullDescription         | string  | Полное описание проекта                                                                        |
#      | dfaId                   | string  | наименование ЦФА                                                                               |
#      | issueDecisionForm       | string  | Pdf-документ: Решение о выпуске                                                                |
#      | issueDecisionAcceptance | boolean | Признак: Подтверждаю, что ознакомился со сформированным на платформе ИС решением о выпуске ЦФА |
#      | issuerAddress           | string  | Адрес эмитента, куда зачислять средства после исполнения ICO                                   |

#    And smart contract template for "ICO" state model is:
#      | id                     | typeId  | description                                   |
#      | totalPurchased         | numeric | Собрано средств на платформе                  |
#      | subscriptionPercentage | numeric | Подписка (процент выкупленных ЦФА от HardCap) |
#      | investorsCount         | numeric | Количество инвесторов                         |
#      | purchasedDfaCount      | numeric | Количество выкупленных ЦФА                    |
#      | tokensReleased         | boolean | Токены распределены                           |
#      | contractLocked         | boolean | Признак доступности инвестирования            |
#      | sales                  | object  | Кому проданы токены                           |

    And "SBER" registered smart contract template for "ICO"

    When "ICOIssuer" registered address for smart contract "ICO"

    When "SBER" registered token type "SBC"

    And smart contract "ICO" regulators are:
      | name      | capabilities |
      | Regulator | ALL          |

    And "ICOIssuer" registered "ICO" smart contract with smart contract template "ICO" and attributes:
      | key                   | value     |
      | investmentTokenType   | SBC       |
      | issuerAddress         | undefined |
      | hardcapSum            | 264       |
      | softcapSum            | 100       |
      | investmentCoefficient | 33        |
      | subscriptionStartDate | 2022-02-22T10:00:00.0Z |
      | subscriptionEndDate   | 2022-02-22T10:10:00.0Z |
      | dfaAllocationDate     | 2022-02-22T10:20:00.0Z |


    When "Regulator" approves "ICO" smart contract

    When "SBER" checks "ICO" smart contract state

    When "ICOIssuer" checks "ICO" smart contract regulation

  Scenario: Negative scenario
    Given There is a client "SBER"

    Given There is a client "Regulator"

    Given There is a client "ICOIssuer"

#    When smart contract template for "ICO" attributes are:
#      | id                      | typeId  | description                                                                                    |
#      | dfaName                 | string  | Наименование ЦФА                                                                               |
#      | dfaAlias                | string  | Уникальный код ЦФА (сокращенное название ЦФА)                                                  |
#      | series                  | string  | Серия выпуска                                                                                  |
#      | dfaRepaymentPrice       | numeric | Объём прав при погашении (Цена одного ЦФА при погашении)                                       |
#      | repaymentDate           | string  | Дата погашения                                                                                 |
#      | paymentCurrency         | string  | Средство участия (например: rub, SBC)                                                          |
#      | softcapSum              | numeric | Объем Soft Cap                                                                                 |
#      | limitsUsage             | boolean | Признак: не использовать предельные значения для Hard Cap и лота на операцию                   |
#      | hardcapSum              | numeric | Объем Hard Cap. Не передается, если limitsUsage=True                                           |
#      | maxTransactionSum       | numeric | Объем максимального лота на операцию. Не передается, если limitsUsage=True                     |
#      | dfaPurchasePrice        | numeric | Цена приобретения одного ЦФА при выпуске                                                       |
#      | priceType               | string  | Порядок определения цены                                                                       |
#      | dfaAllocation           | string  | Распределение ЦФА                                                                              |
#      | subscriptionStartDate   | string  | Дата начала подписки                                                                           |
#      | subscriptionEndDate     | string  | Дата окночания подписки                                                                        |
#      | dfaAllocationDate       | string  | Итоговое распределение ЦФА                                                                     |
#      | projectCover            | string  | Изображение: Обложка проекта                                                                   |
#      | projectImage            | string  | Изображение: Логотип проекта                                                                   |
#      | companyWebsite          | string  | Веб-сайт компании                                                                              |
#      | shortDescription        | string  | Краткое описание проекта                                                                       |
#      | fullDescription         | string  | Полное описание проекта                                                                        |
#      | dfaId                   | string  | наименование ЦФА                                                                               |
#      | issueDecisionForm       | string  | Pdf-документ: Решение о выпуске                                                                |
#      | issueDecisionAcceptance | boolean | Признак: Подтверждаю, что ознакомился со сформированным на платформе ИС решением о выпуске ЦФА |
#      | investmentCoefficient   | numeric | Коэффициент инвестирования                                                                     |
#      | issuerAddress           | string  | Адрес эмитента, куда зачислять средства после исполнения ICO                                   |
#
#    And smart contract template for "ICO" state model is:
#      | id                     | typeId  | description                                   |
#      | totalPurchased         | numeric | Собрано средств на платформе                  |
#      | subscriptionPercentage | numeric | Подписка (процент выкупленных ЦФА от HardCap) |
#      | investorsCount         | numeric | Количество инвесторов                         |
#      | purchasedDfaCount      | numeric | Количество выкупленных ЦФА                    |
#      | tokensReleased         | boolean | Токены распределены                           |
#      | contractLocked         | boolean | Признак доступности инвестирования            |
#      | sales                  | object  | Кому проданы токены                           |

    And "SBER" registered smart contract template for "ICO"

    When "ICOIssuer" registered address for smart contract "ICO"

    When "SBER" registered token type "SBC"

    And smart contract "ICO" regulators are:
      | name      | capabilities |
      | Regulator | ALL          |

    And "ICOIssuer" registered "ICO" smart contract with smart contract template "ICO" and attributes:
      | key                   | value     |
      | investmentTokenType   | SBC       |
      | issuerAddress         | undefined |
      | hardcapSum            | 264       |
      | softcapSum            | 100       |
      | investmentCoefficient | 33        |
      | subscriptionStartDate | 2022-02-22T10:00:00.0Z |
      | subscriptionEndDate   | 2022-02-22T10:10:00.0Z |
      | dfaAllocationDate     | 2022-02-22T10:20:00.0Z |

    When "Regulator" rejects smart contract "ICO" by "ICOIssuer"

    When "ICOIssuer" checks "ICO" smart contract regulation

  Scenario: Unable to use smart contract before regulation
    Given There is a client "SBER"

    Given There is a client "ICOIssuer"

    Given There is a client "Client1"

    Given There is a client "Regulator"

#    ###
#    When smart contract template for "ICO" attributes are:
#      | id                      | typeId  | description                                                                                    |
#      | dfaName                 | string  | Наименование ЦФА                                                                               |
#      | dfaAlias                | string  | Уникальный код ЦФА (сокращенное название ЦФА)                                                  |
#      | series                  | string  | Серия выпуска                                                                                  |
#      | dfaRepaymentPrice       | numeric | Объём прав при погашении (Цена одного ЦФА при погашении)                                       |
#      | repaymentDate           | string  | Дата погашения                                                                                 |
#      | paymentCurrency         | string  | Средство участия (например: rub, SBC)                                                          |
#      | softcapSum              | numeric | Объем Soft Cap                                                                                 |
#      | limitsUsage             | boolean | Признак: не использовать предельные значения для Hard Cap и лота на операцию                   |
#      | hardcapSum              | numeric | Объем Hard Cap. Не передается, если limitsUsage=True                                           |
#      | maxTransactionSum       | numeric | Объем максимального лота на операцию. Не передается, если limitsUsage=True                     |
#      | dfaPurchasePrice        | numeric | Цена приобретения одного ЦФА при выпуске                                                       |
#      | priceType               | string  | Порядок определения цены                                                                       |
#      | dfaAllocation           | string  | Распределение ЦФА                                                                              |
#      | subscriptionStartDate   | string  | Дата начала подписки                                                                           |
#      | subscriptionEndDate     | string  | Дата окночания подписки                                                                        |
#      | dfaAllocationDate       | string  | Итоговое распределение ЦФА                                                                     |
#      | projectCover            | string  | Изображение: Обложка проекта                                                                   |
#      | projectImage            | string  | Изображение: Логотип проекта                                                                   |
#      | companyWebsite          | string  | Веб-сайт компании                                                                              |
#      | shortDescription        | string  | Краткое описание проекта                                                                       |
#      | fullDescription         | string  | Полное описание проекта                                                                        |
#      | dfaId                   | string  | наименование ЦФА                                                                               |
#      | issueDecisionForm       | string  | Pdf-документ: Решение о выпуске                                                                |
#      | issueDecisionAcceptance | boolean | Признак: Подтверждаю, что ознакомился со сформированным на платформе ИС решением о выпуске ЦФА |
#      | investmentCoefficient   | numeric | Коэффициент инвестирования                                                                     |
#      | issuerAddress           | string  | Адрес эмитента, куда зачислять средства после исполнения ICO                                   |
#
#    And smart contract template for "ICO" state model is:
#      | id                     | typeId  | description                                   |
#      | totalPurchased         | numeric | Собрано средств на платформе                  |
#      | subscriptionPercentage | numeric | Подписка (процент выкупленных ЦФА от HardCap) |
#      | investorsCount         | numeric | Количество инвесторов                         |
#      | purchasedDfaCount      | numeric | Количество выкупленных ЦФА                    |
#      | tokensReleased         | boolean | Токены распределены                           |
#      | contractLocked         | boolean | Признак доступности инвестирования            |
#      | sales                  | object  | Кому проданы токены                           |

    And "SBER" registered smart contract template for "ICO"

    When "ICOIssuer" registered address for smart contract "ICO"

    When "SBER" registered token type "SBC"

    And smart contract "ICO" burn extra data:
      | id            | typeId  | description                 |
      | accountNumber | numeric | Номер аккаунта пользователя |

    And smart contract "ICO" regulators are:
      | name      | capabilities |
      | Regulator | ALL          |

    And "ICOIssuer" registered "ICO" smart contract with smart contract template "ICO" and attributes:
      | key                   | value     |
      | investmentTokenType   | SBC       |
      | issuerAddress         | undefined |
      | hardcapSum            | 264       |
      | softcapSum            | 100       |
      | investmentCoefficient | 33        |
      | subscriptionStartDate | 2022-02-22T10:00:00.0Z |
      | subscriptionEndDate   | 2022-02-22T10:10:00.0Z |
      | dfaAllocationDate           | 2022-02-22T10:20:00.0Z |

      ###

    When "SBER" checks "ICO" smart contract state

    When "ICOIssuer" checks "ICO" smart contract regulation

    ###

    When "SBER" issued token "T1" for "Client1" of type "SBC" with value "100"

    When "Client1" sees one token in his list

    When "Client1" sends his "T1" token to "ICO" to unregulated smart contract
