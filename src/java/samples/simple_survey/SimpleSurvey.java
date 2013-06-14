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


package simple_survey;

import com.amazonaws.mturk.addon.HITQuestion;
import com.amazonaws.mturk.requester.Comparator;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.requester.Locale;
import com.amazonaws.mturk.requester.QualificationRequirement;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.util.PropertiesClientConfig;

/**
 * The Simple Survey sample application will create a HIT asking a worker to indicate their 
 * political party preferences.
 * 
 * mturk.properties must be found in the current file path.
 * 
 * The following concepts are covered:
 * - File based QAP HIT loading
 * - Using a locale qualification
 */
public class SimpleSurvey {

	private RequesterService service;
	
  //Defining the attributes of the HIT
  private String title = "What is your political preference?";
  private String description = "This is a simple survey HIT created by MTurk SDK.";
  private int numAssignments = 1;
  private double reward = 0.05;
  private String keywords = "sample, SDK, survey";
  private long assignmentDurationInSeconds = 60 * 60; // 1 hour
  private long autoApprovalDelayInSeconds = 60 * 60 * 24 * 15; // 15 days
  private long lifetimeInSeconds = 60 * 60 * 24 * 3; // 3 days
  private String requesterAnnotation = "sample#survey";
  
  //Defining the location of the externalized question (QAP) file.
  private String rootDir = ".";
  private String questionFile = rootDir + "/simple_survey.question";
  
  /**
   * Constructor
   *
   */
  public SimpleSurvey() {
    service = new RequesterService(new PropertiesClientConfig("../mturk.properties"));
  }
	
  /**
   * Checks to see if there are sufficient funds in your account to run this sample.
   * @return true if there are sufficient funds.  False if not.
   */
  public boolean hasEnoughFund() {
    double balance = service.getAccountBalance();
    System.out.println("Got account balance: " + RequesterService.formatCurrency(balance));
    return balance > 0;
  }
  
  /**
   * Creates the simple survey.
   *
   */
  public void createSimpleSurvey() {
    try {

      // This is an example of creating a qualification.
      // This is a built-in qualification -- user must be based in the US
      QualificationRequirement qualReq = new QualificationRequirement();
      qualReq.setQualificationTypeId(RequesterService.LOCALE_QUALIFICATION_TYPE_ID);
      qualReq.setComparator(Comparator.EqualTo);
      Locale country = new Locale();
      country.setCountry("US");
      qualReq.setLocaleValue(country);

      // The create HIT method takes in an array of QualificationRequirements
      // since a HIT can have multiple qualifications.
      QualificationRequirement[] qualReqs = null;
      qualReqs = new QualificationRequirement[] { qualReq };

      // Loading the question (QAP) file. HITQuestion is a helper class that
      // contains the QAP of the HIT defined in the external file. This feature 
      // allows you to write the entire QAP externally as a file and be able to 
      // modify it without recompiling your code.
      HITQuestion question = new HITQuestion(questionFile);
      
      //Creating the HIT and loading it into Mechanical Turk
      HIT hit = service.createHIT(null, // HITTypeId 
          title, 
          description, keywords, 
          question.getQuestion(),
          reward, assignmentDurationInSeconds,
          autoApprovalDelayInSeconds, lifetimeInSeconds,
          numAssignments, requesterAnnotation, 
          qualReqs,
          null // responseGroup
        );
      
      System.out.println("Created HIT: " + hit.getHITId());

      System.out.println("You may see your HIT with HITTypeId '" 
          + hit.getHITTypeId() + "' here: ");
      
      System.out.println(service.getWebsiteURL() 
          + "/mturk/preview?groupId=" + hit.getHITTypeId());
      
      //Demonstrates how a HIT can be retrieved if you know its HIT ID
      HIT hit2 = service.getHIT(hit.getHITId());
      
      System.out.println("Retrieved HIT: " + hit2.getHITId());
      
      if (!hit.getHITId().equals(hit2.getHITId())) {
        System.err.println("The HIT Ids should match: " 
            + hit.getHITId() + ", " + hit2.getHITId());
      }
      
    } catch (Exception e) {
      System.err.println(e.getLocalizedMessage());
    }
  }
  
	/**
   * @param args
   */
	public static void main(String[] args) {

    SimpleSurvey app = new SimpleSurvey();

    if (app.hasEnoughFund()) {
      app.createSimpleSurvey();
    }
  }
}
