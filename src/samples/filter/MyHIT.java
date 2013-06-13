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


package filter;

import com.amazonaws.mturk.filter.Filter;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.ServiceException;
import com.amazonaws.mturk.util.PropertiesClientConfig;

public class MyHIT {
    
    //  Defining the attributes of the HIT to be created
    private String title = "Answer the question";
    private String description = 
      "This is a HIT created by the Mechanical Turk SDK.  Please answer the provided question.";
    private int numAssignments = 1;
    private double reward = 0.01;
    private RequesterService service = null;
    
    public MyHIT() {
        service = new RequesterService(new PropertiesClientConfig("../mturk.properties"));
    }
    
    public void appendFilter(Filter filter) {
        service.addFilter(filter);
    }
    
    public void createMyHIT() {
        try {

            // The createHIT method is called using a convenience static method of
            // RequesterService.getBasicFreeTextQuestion that generates the QAP for
            // the HIT.
            HIT hit = service.createHIT(
                    title,
                    description,
                    reward,
                    RequesterService.getBasicFreeTextQuestion(
                        "What is the current temperature now in Seattle, WA?"),
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
     * @param args
     */
    public static void main(String[] args) {
        MyHIT myHIT = new MyHIT();
        myHIT.appendFilter(new AppendKeywordFilter());//append the filter  
        myHIT.createMyHIT();
    }


}
