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
import com.amazonaws.mturk.filter.Message;
import com.amazonaws.mturk.filter.Reply;
import com.amazonaws.mturk.requester.CreateHITRequest;
import com.amazonaws.mturk.service.exception.ServiceException;

/**
 * This filter extends the Filter class and implements the execute method. 
 * It checks if the createHIT operation is being called and appends 'myHIT' to set of keywords of the HIT being created  
 *
 */
public class AppendKeywordFilter extends Filter {

    /**
     * Checks if method is createHIT and appends 'myHIT' to the set of keywords
     */
    @Override
    public Reply execute(Message m) throws ServiceException {
        if (m.getMethodName().equals("CreateHIT")) {
            CreateHITRequest[] requestArray = (CreateHITRequest[]) m.getRequests();
            for (CreateHITRequest request : requestArray) {
                StringBuffer keywords = new StringBuffer();
                //append existing keywords to string buffer 
                if (request.getKeywords() != null) {
                    keywords.append(request.getKeywords());
                    keywords.append(", ");
                }
                keywords.append("myHIT");
                request.setKeywords(keywords.toString());
            }
        }
        //pass the message to the next filter
        return passMessage(m);
    }

    
}
