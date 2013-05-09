# given a Survey object
# post survey, according to the format given (questions in the order they're provided, options in the order they're provided)
# get survey answers
# function to return survey answers as responses (list of tuples of question object and option list)

# so basically, continually run on some port provided as input. 
# process get and post requests
# note : if you want to post each quesiton as a html file, you will need to keep cookies to maintain your session
# another way to handle this is to write javascript to update the page
# if you go the static web page route, then you need to accumulate answers as the person goes. 
# if you respond to user input indicating that a question has been answered, then you can get all of the answers back at once
# while the former is easier to code up in the short term, it presents problems in terms of session management (we will definitely only be able to hand one survey taker at a time)
# the JS approach might be more of a pain, but it's possible that there are python libraries that compile python to JS

#!/usr/bin/python

import BaseHTTPServer
import CGIHTTPServer
from questionnaire import *

def create_forms(survey):
    for i in range(len(survey.questions)):
        outfile = open("cgi-bin/q"+str(i)+".py", "w")
        outfile.write('#!/usr/bin/python\n\n')
        outfile.write('print "Content-type: text/html"\n')
        outfile.write('print\n')
        outfile.write('print """\n')
        outfile.write('<form method="post" action="q'+str(i+1)+'.py" target="_blank">')
        outfile.write('<p>'+survey.questions[i].qtext+'</p>\n')
        if (survey.questions[i].qtype == qtypes["freetext"] ):
            outfile.write('<input type="text" name="data"><br />\n')
        elif (survey.questions[i].qtype == qtypes["radio"] ):
            for j in range(len(survey.questions[i].options)):
                outfile.write('<input type="radio" name="o'+str(j)+'" value="on" \>'+survey.questions[i].options[j].otext+'<br />\n')
        elif (survey.questions[i].qtype == qtypes["check"] ):
            for j in range(len(survey.questions[i].options)):
                outfile.write('<input type="checkbox" name="o'+str(j)+'" value="on" \>'+survey.questions[i].options[j].otext+'<br />\n')
                pass
        elif (survey.questions[i].qtype == qtypes["dropdown"] ):
            outfile.write('<select name="dropdown">\n')
            for j in range(len(survey.questions[i].options)):
                outfile.write('<option value="'+survey.questions[i].options[j].otext+'" selected>'+survey.questions[i].options[j].otext+'</option>\n')
            outfile.write('</select>\n')
        outfile.write('<input type="submit" value = "Submit" />\n')
        outfile.write('</form>\n');
        outfile.write('"""\n')
    return

def serve(port, survey):
    server = BaseHTTPServer.HTTPServer
    handler = CGIHTTPServer.CGIHTTPRequestHandler
    server_address = ("", port)
    #create_forms(survey)
    
 
    httpd = server(server_address, handler)
    httpd.serve_forever()
    return
    
if __name__=="__main__":
    qs = [q1, q2, q3, q4]
    survey = Survey(qs)
    serve(8000, survey)