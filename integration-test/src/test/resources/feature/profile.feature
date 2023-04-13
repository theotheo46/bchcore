@skip
Feature: Public profile functionality

  Scenario: Positive scenario

    Given [NFT] there is a client "NFT"

    Given [NFT] there is a client "NFT_2"

    When [NFT] client "NFT" can create a profile "test_name"

    When [NFT] client "NFT" can get his profile "test_name"

    When [NFT] client "NFT" can not update his profile name "test_name" and member id

    When [NFT] client "NFT" can update his profile "test_name"

    When [NFT] client "NFT" can list his profile "test_name" and other

    When [NFT] client "NFT" can register token type "Diploma"

    When [NFT] client "NFT" can issue token "CertOfHonor" with type "Diploma" and value "100"

    When [NFT] client "NFT" can link token "CertOfHonor" to his profile "test_name"

    When [NFT] client "NFT" can link token "CertOfHonor" to his profile "Test Profile" and get token profiles

    When [NFT] client "NFT" can unlink token "CertOfHonor" from his profile "test_name"

    When [NFT] client "NFT" creates a profile "Profile_1" and issues token "token_1" and client "NFT_2" creates a profile "Profile_2" and issues token "token_2"
