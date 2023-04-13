Feature: Public endorsement

  Scenario: Endorse member
    Given There is a client "Client1"
    Given There is a client "Endorser1"
    Given There is a client "Endorser2"
    When "Endorser1" public endorse member "Client1" with endorsement "Data1" of kind "kind1"
    When "Client1" checks his public endorsements
    When "Endorser2" public endorse member "Client1" with endorsement "Data2" of kind "kind2"
    When "Client1" checks his public endorsements


  Scenario: Multiple endorsements
    Given There is a client "Client1"
    Given There is a client "Endorser1"
    Given There is a client "Endorser2"
    When "Endorser1" public endorse member "Client1" with endorsement "Data1" of kind "kind1"
    When "Client1" checks his public endorsements
    When "Endorser2" public endorse member "Client1" with endorsement "Data2" of kind "kind1"
    When "Client1" checks his public endorsements
    When "Endorser1" public endorse member "Client1" with endorsement "DataNew" of kind "kind1"
    When "Client1" checks his public endorsements


  Scenario: Revoke endorsement
    Given There is a client "Client1"
    Given There is a client "Endorser1"
    When "Endorser1" public endorse member "Client1" with endorsement "Data1" of kind "kind1"
    When "Client1" checks his public endorsements
    When "Endorser1" public endorse member "Client1" with endorsement "Data2" of kind "kind2"
    When "Client1" checks his public endorsements
    When "Endorser1" revoke endorsement of kind "kind1" for member "Client1"
    When "Client1" checks his public endorsements

