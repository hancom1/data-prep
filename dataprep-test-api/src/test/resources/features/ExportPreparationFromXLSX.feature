@EnvOS @EnvOnPremise @EnvCloud
Feature: Export Preparation from XLSX file

  Scenario: Create a preparation with one step from a XLSX
    Given I upload the dataset "/data/6L3C.xlsx" with name "6L3C_dataset"
    Then I wait for the dataset "6L3C_dataset" metadata to be computed
    And I create a preparation with name "6L3C_preparation", based on "6L3C_dataset" dataset
    And I add a "uppercase" step on the preparation "6L3C_preparation" with parameters :
      | column_name | lastname |
      | column_id   | 0001     |

  Scenario: Verify transformation result
    # escape and enclosure characters should be given because they can be empty
    When I export the preparation with parameters :
      | exportType           | CSV              |
      | preparationName      | 6L3C_preparation |
      | csv_escape_character | "                |
      | csv_enclosure_char   | "                |
      | fileName             | 6L3C_result.csv  |
    Then I check that "6L3C_result.csv" temporary file equals "/data/6L3C_default_export_parameters.csv" file

  Scenario: Verify transformation result with another escape char export on 6L3C_dataset from XLSX file
    When I export the preparation with parameters :
      | exportType           | CSV              |
      | preparationName      | 6L3C_preparation |
      | csv_escape_character | #                |
      | csv_enclosure_char   | "                |
      | fileName             | 6L3C_result.csv  |
    Then I check that "6L3C_result.csv" temporary file equals "/data/6L3C_processed_custom_escape_char.csv" file

  @CleanAfter
  Scenario: Verify transformation result with custom parameters export on 6L3C_dataset from XLSX file
    When I export the preparation with parameters :
      | exportType           | CSV                               |
      | csv_fields_delimiter | -                                 |
      | csv_escape_character | #                                 |
      | csv_enclosure_mode   | all_fields                        |
      | csv_charset          | ISO-8859-1                        |
      | csv_enclosure_char   | +                                 |
      | preparationName      | 6L3C_preparation                  |
      | fileName             | 6L3C_result_with_custom_param.csv |
    Then I check that "6L3C_result_with_custom_param.csv" temporary file equals "/data/6L3C_exported_with_custom_param.csv" file
