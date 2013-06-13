#!/usr/bin/python

import cgi
from uuid import uuid1
from questionnaire import *
import anydbm

qs = [q1, q2, q3, q4]
print '"Content-type: text/html"\n'

def generate_question(question, ind, shown, session_key):
    print '<html>\n'
    print '<head>\n'
    print '<title>Survey</title>\n'
    print '<form method="post" action="cgi_processor.py" target="_blank">\n'
    print '<input type="hidden" name="index" value="'+str(ind)+'">\n'
    print '<input type="hidden" name="session_key" value="'+str(session_key)+'">\n'
    print '<input type="hidden" name="quid" value="'+str(question.quid)+'">\n'
    print '<input type="hidden" name="shown" value="'+shown+'">\n'
    print '<p>'+question.qtext+'</p>\n'
    if (question.qtype == qtypes["freetext"] ):
        print '<input type="text" name="data"><br />\n'
    elif (question.qtype == qtypes["radio"] ):
        for j in range(len(question.options)):
            print '<input type="radio" name="ro'+str(j)+'" value="on" \>'+question.options[j].otext+'<br />\n'
    elif (question.qtype == qtypes["check"] ):
        for j in range(len(question.options)):
            print '<input type="checkbox" name="co'+str(j)+'" value="on" \>'+question.options[j].otext+'<br />\n'
            pass
    elif (question.qtype == qtypes["dropdown"] ):
        print '<select name="dropdown">\n'
        for j in range(len(question.options)):
            print '<option value="'+question.options[j].otext+'" selected>'+question.options[j].otext+'</option>\n'
        print '</select>\n'
    print '<input type="submit" value = "Submit" />\n'
    print '</form>\n'
    print '</body>\n'
    print '</html>\n'
    return
    
def create_session():
    session_file = open('sessions.txt', 'a')
    session_key = uuid1()
    session_file.write(str(session_key)+'\n')
    session_file.close()
    return session_key
    
def delete_session(id):
    
    return
    
def write_to_db(session_key, quid, response):
    file = open(str(session_key)+'.txt', 'a')
    file.write(quid+',' + '{'+str(response)+'}\n');
            
    return
    
def create_database(qs, session_key):
    file = open(session_key+'_questions.txt', 'w')
    for q in qs:
        file.write("quid: " + str(q.quid))
        file.write(q.qtext)
        for o in q.options:
            file.write(o.oid)
            file.write(o.otext)
    return
    
def get_next_question(session_key, index):
    file = open(session_key+'_questions.txt', 'r')
    counter = -1
    quid = ""
    
    for line in file:
        if ("quid: " in line):
            counter=counter+1
        if (counter == index):
            quid = line.split()[1]
    return
    
        
def main():
    for q in qs:
        print >> sys.stderr, q.quid
    print >> sys.stderr, '\n'
    form = cgi.FieldStorage()
    if (form.has_key("session_key")):
        session_key = form["session_key"].value
        quid = form["quid"].value
        write_to_db(session_key, quid, 0)
        index = int(form["index"].value)
        #next_question = 
        #else:
        print "You're done!"
    else:
        #qs = launcher.request_survey().questions
        session_key = create_session()
        create_database(qs, session_key)
        generate_question(qs[0], 0, session_key)
        
        
        
        
    
    
    
    
    return
    
main()