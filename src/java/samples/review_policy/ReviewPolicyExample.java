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


package review_policy;

import java.util.HashSet;
import java.util.Arrays;
import java.util.Set;

import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.ServiceException;
import com.amazonaws.mturk.util.PropertiesClientConfig;
import com.amazonaws.mturk.requester.Assignment;
import com.amazonaws.mturk.requester.DataPoint;
import com.amazonaws.mturk.requester.GetReviewResultsForHITResult;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.requester.ParameterMapEntry;
import com.amazonaws.mturk.requester.PolicyParameter;
import com.amazonaws.mturk.requester.RequesterStatistic;
import com.amazonaws.mturk.requester.ReviewActionDetail;
import com.amazonaws.mturk.requester.ReviewPolicy;
import com.amazonaws.mturk.requester.ReviewPolicyLevel;
import com.amazonaws.mturk.requester.ReviewReport;
import com.amazonaws.mturk.requester.ReviewResultDetail;
import com.amazonaws.mturk.requester.TimePeriod;

/**
 * The MTurk Review Policy sample application uses the Java SDK to create a HIT
 * with review policies. mturk.properties must be found in the current file path.
 */
public class ReviewPolicyExample {
  
  private RequesterService service;
  
  // Defining the attributes of the HIT to be created
  private String title = "Answer some questions";
  private String description = 
    "This is a HIT created by the Mechanical Turk SDK.  Please answer the provided questions.";
  private double reward = 0.05;
  
  /**
   * Constructor
   * 
   */
  public ReviewPolicyExample() {
    service = new RequesterService(new PropertiesClientConfig("../mturk.properties"));
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
    return balance >= reward;
  }
  
  /**
   * Creates a HIT with both assignment-level and HIT-level review policies.
   * 
   */
  public void createHITWithReviewPolicies() {
    try {
      // Create a HIT type for our HIT
      String hitTypeId = service.registerHITType(
          RequesterService.DEFAULT_AUTO_APPROVAL_DELAY_IN_SECONDS,
          RequesterService.DEFAULT_ASSIGNMENT_DURATION_IN_SECONDS,
          reward,
          title,
          null, // keywords
          description,
          null); // qualRequirements
      
      // Set up a ScoreKnownAnswers policy
      ParameterMapEntry[] answerKey = {
          new ParameterMapEntry("q1", new String[] {"2"}),
          new ParameterMapEntry("q2", new String[] {"4"})
      };
      PolicyParameter[] skaParams = {
          new PolicyParameter("ApproveIfKnownAnswerScoreIsAtLeast", new String[] {"100"}, null),
          new PolicyParameter("ApproveReason", (String[]) Arrays.asList("You can count").toArray(), null),
          new PolicyParameter("RejectIfKnownAnswerScoreIsLessThan", new String[] {"50"}, null),
          new PolicyParameter("RejectReason", (String[]) Arrays.asList("You flunked math").toArray(), null),
          new PolicyParameter("ExtendIfKnownAnswerScoreIsLessThan", new String[] {"50"}, null),
          new PolicyParameter("AnswerKey", null, answerKey)
      };
      ReviewPolicy scoreKnownAnswersPolicy = new ReviewPolicy("ScoreMyKnownAnswers/2011-09-01", skaParams);
      
      // Set up a PluralityHitReview policy
      PolicyParameter[] phrParams = {
          new PolicyParameter("QuestionIds", new String[] {"q3"}, null),
          new PolicyParameter("QuestionAgreementThreshold", new String[] {"49"}, null),
          new PolicyParameter("ExtendIfHITAgreementScoreIsLessThan", new String[] {"100"}, null),
          new PolicyParameter("ExtendAssignments", new String[] {"1"}, null),
          new PolicyParameter("ExtendMaximumAssignments ", new String[] {"10"}, null),
          new PolicyParameter("ApproveIfWorkerAgreementScoreIsAtLeast", new String[] {"100"}, null),
          new PolicyParameter("RejectIfWorkerAgreementScoreIsLessThan", new String[] {"100"}, null)
      };
      ReviewPolicy pluralityHitReviewPolicy = new ReviewPolicy("SimplePlurality/2011-09-01", phrParams);
      
      // Set up some custom question XML
      String question =
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<QuestionForm xmlns=\"http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2005-10-01/QuestionForm.xsd\">"
        + "  <Question>"
        + "    <QuestionIdentifier>q1</QuestionIdentifier>"
        + "    <QuestionContent>"
        + "      <Text>What is 1 + 1?</Text>"
        + "    </QuestionContent>"
        + "    <AnswerSpecification>"
        + "      <FreeTextAnswer/>"
        + "    </AnswerSpecification>" 
        + "  </Question>"
        + "  <Question>"
        + "    <QuestionIdentifier>q2</QuestionIdentifier>"
        + "    <QuestionContent>"
        + "      <Text>What is 2 + 2?</Text>"
        + "    </QuestionContent>"
        + "    <AnswerSpecification>"
        + "      <FreeTextAnswer/>"
        + "    </AnswerSpecification>" 
        + "  </Question>"
        + "  <Question>"
        + "    <QuestionIdentifier>q3</QuestionIdentifier>"
        + "    <QuestionContent>"
        + "      <Text>What color is the sky?</Text>"
        + "    </QuestionContent>"
        + "    <AnswerSpecification>"
        + "      <FreeTextAnswer/>"
        + "    </AnswerSpecification>" 
        + "  </Question>"
        + "</QuestionForm>";
      
      // Actually create the HIT with the review policy
      HIT hit = service.createHIT(
          hitTypeId, // hitTypeId
          title,
          description,
          null, // keywords
          question,
          reward,
          RequesterService.DEFAULT_ASSIGNMENT_DURATION_IN_SECONDS,
          RequesterService.DEFAULT_AUTO_APPROVAL_DELAY_IN_SECONDS,
          RequesterService.DEFAULT_LIFETIME_IN_SECONDS,
          3, // maxAssignments
          null, // requesterAnnotation
          null, // qualificationRequirements
          null, // responseGroup
          null, // uniqueRequestToken
          scoreKnownAnswersPolicy,
          pluralityHitReviewPolicy);
      
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
   * Helper function for printReviewResults: prints the contents of a
   * ReviewReport object.
   * @param report The ReviewReport object to print
   * @param policyName The name of the policy associated with the given report
   */
  private void printReviewReport(ReviewReport report, String policyName) {
    if (report == null) {
      System.out.println("No data for policy " + policyName);
    } else {
      // Print review results
      ReviewResultDetail[] results = report.getReviewResult();
      if (results == null) {
        System.out.println("No results for policy " + policyName);
      } else {
        System.out.println("Results for policy " + policyName + ":");
        for (ReviewResultDetail result : results) {
          String subjectType = result.getSubjectType();
          String subjectId = result.getSubjectId();
          String questionId = result.getQuestionId();
          String key = result.getKey();
          String value = result.getValue();
          if (questionId == null || questionId.equals("")) {
            System.out.println("- " + subjectType + " " + subjectId + ": " + key + " is " + value);
          } else {
            System.out.println("- " + subjectType + " " + subjectId + ": " + key + " for " + questionId + " is " + value);
          }
        }
      }
      System.out.println();
      
      // Print review actions
      ReviewActionDetail[] actions = report.getReviewAction();
      if (actions == null) {
        System.out.println("No actions for policy " + policyName);
      } else {
        System.out.println("Actions for policy " + policyName + ":");
        for (ReviewActionDetail action : actions) {
          String actionName = action.getActionName();
          String actionStatus = action.getStatus().toString();
          String objectId = action.getObjectId();
          String objectType = action.getObjectType();
          String resultDescription = action.getResult();
          System.out.println("- Action " + actionName + " on " + objectType + " " + objectId + ": " + actionStatus);
          System.out.println("  Result: " + resultDescription);
        }
      }
      System.out.println();
    }
  }
  
  /**
   * Helper function: returns a life-to-date statistic for the specified worker
   * @param workerId
   * @param statistic
   * @return
   */
  private String getStatistic(String workerId, RequesterStatistic statistic) {
    DataPoint[] stat =
      service.getRequesterWorkerStatistic(
          statistic,
          TimePeriod.LifeToDate,
          workerId,
          null, // count
          new String[] {"Minimal"}); // responseGroup
    if (stat[0].getLongValue() != null) {
        return stat[0].getLongValue().toString();
    } else {
        return stat[0].getDoubleValue().toString();
    }
  }
  
  /**
   * Fetches and prints a bunch of review policy info about the given HIT.
   * @param hitId Which HIT to print results for
   */
  public void printReviewResults(String hitId) {
    service.getRequesterStatistic(RequesterStatistic.EstimatedTotalLiability, TimePeriod.LifeToDate, null);
    
    System.out.println("Getting review policy results for HIT " + hitId + "...");
    System.out.println();
    GetReviewResultsForHITResult results = service.getReviewResultsForHIT(
        hitId,
        new ReviewPolicyLevel[] {ReviewPolicyLevel.Assignment, ReviewPolicyLevel.HIT},
        true, // retrieveActions
        true, // retrieveResults
        1, // pageNumber
        1000, // pageSize
        null); // responseGroup
    
    // Print ScoreKnownAnswers results/actions for this HIT
    printReviewReport(results.getAssignmentReviewReport(), results.getAssignmentReviewPolicy().getPolicyName());
    
    // Print PluralityHitReview results/actions for this HIT
    printReviewReport(results.getHITReviewReport(), results.getHITReviewPolicy().getPolicyName());
    
    // Get a list of workers who worked on this HIT
    Assignment[] assignments = service.getAssignmentsForHIT(hitId, 1);
    Set<String> workerIds = new HashSet<String>();
    if (assignments != null) {
      for (Assignment assignment : assignments) {
        workerIds.add(assignment.getWorkerId());
      }
    }
    
    // For each of those workers, fetch statistics relevant to review policies
    RequesterStatistic[] statistics = { // which statistics to print
        RequesterStatistic.NumberKnownAnswersCorrect,
        RequesterStatistic.NumberKnownAnswersIncorrect,
        RequesterStatistic.NumberKnownAnswersEvaluated,
        RequesterStatistic.PercentKnownAnswersCorrect,
        RequesterStatistic.NumberPluralityAnswersCorrect,
        RequesterStatistic.NumberPluralityAnswersIncorrect,
        RequesterStatistic.NumberPluralityAnswersEvaluated,
        RequesterStatistic.PercentPluralityAnswersCorrect
    };
    if (workerIds.size() == 0) {
      System.out.println("No workers worked on this HIT.");
    } else {
      System.out.println("Review-policy-related statistics follow.");
      System.out.println("These statistics are life-to-date counts for each worker who worked on this HIT.");
      System.out.println("These counts only include work done for the requester of the HIT.");
      for (String workerId : workerIds) {
        System.out.println("Worker " + workerId + ":");
        for (RequesterStatistic statistic : statistics) {
          System.out.println("- " + statistic.getValue() + ": " + getStatistic(workerId, statistic));
        }
      }
    }
  }
  
  /**
   * Main method
   * 
   * @param args
   */
  public static void main(String[] args) {
    
    ReviewPolicyExample app = new ReviewPolicyExample();
    
    if (args.length == 1) {
      // Get results for the specified HIT id
      app.printReviewResults(args[0]);
    } else {
      // Try to create a HIT with review policies
      if (app.hasEnoughFund()) {
        app.createHITWithReviewPolicies();
        System.out.println("Success.");
      } else {
        System.out.println("You do not have enough funds to create the HIT.");
      }
    }
  }
}
