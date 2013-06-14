/*
 * Copyright 2007-2012 Amazon Technologies, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 * 
 * http://aws.amazon.com/apache2.0
 * 
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */ 


package helloworld;

import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.ServiceException;
import com.amazonaws.mturk.util.PropertiesClientConfig;
import com.amazonaws.mturk.requester.HIT;

/**
 * The MTurk Hello World sample application creates a simple HIT via the Mechanical Turk 
 * Java SDK. mturk.properties must be found in the current file path.
 */
public class MTurkHelloWorld {

  private RequesterService service;

  // Defining the attributes of the HIT to be created
  private String title = "Answer a question";
  private String description = 
    "This is a HIT created by the Mechanical Turk SDK.  Please answer the provided question.";
  private int numAssignments = 1;
  private double reward = 0.05;

  /**
   * Constructor
   * 
   */
  public MTurkHelloWorld() {
    service = new RequesterService(new PropertiesClientConfig("/Users/jnewman/dev/SurveyMan/src//mturk.properties"));
  }

  /**
   * Check if there are enough funds in your account in order to create the HIT
   * on Mechanical Turk
   * 
   * @return true if there are sufficient funds. False if not.
   */
  public boolean hasEnoughFund() {
    double balance = service.getAccountBalance();
    System.out.println("Got account balance: " + RequesterService.formatCurrency(balance));
    return balance > reward;
  }

  /**
   * Creates the simple HIT.
   * 
   */
  public void createHelloWorld() {
    try {

      // The createHIT method is called using a convenience static method of
      // RequesterService.getBasicFreeTextQuestion that generates the QAP for
      // the HIT.
      HIT hit = service.createHIT(
              title,
              description,
              reward,
              RequesterService.getBasicFreeTextQuestion(
                  "What is the weather like right now in Seattle, WA?"),
              numAssignments);

      System.out.println("Created HIT: " + hit.getHITId());

      System.out.println("You may see your HIT with HITTypeId '" 
          + hit.getHITTypeId() + "' here: ");
      System.out.println(service.getWebsiteURL() 
          + "/mturk/preview?groupId=" + hit.getHITTypeId());

    } catch (ServiceException e) {
      System.err.println(e.getLocalizedMessage());
    }
  }

  /**
   * Main method
   * 
   * @param args
   */
  public static void main(String[] args) {

    MTurkHelloWorld app = new MTurkHelloWorld();

    if (app.hasEnoughFund()) {
      app.createHelloWorld();
      System.out.println("Success.");
    } else {
      System.out.println("You do not have enough funds to create the HIT.");
    }
  }
}
