#requires Python 2.7.5
#attempt at survey representation

qtypes = {"freetext" : 0 , "radio" : 1 , "check" : 2 , "dropdown" : 3}


#generate ids for survey components based on prefixes passed as arg
#op=option id, s=survey id, q=question id
class idGenerator:
    def __init__(self, prefix):
        self.numAssigned=0
        self.prefix=prefix
    
    def generateID(self):
        self.numAssigned+=1
        return self.prefix+str(self.numAssigned)

opGen = idGenerator("op")
surveyGen = idGenerator("s")
qGen = idGenerator("q")

class Survey:

    def __init__(self, questions = [], blocklist = [], breakoff = True):
        #generate ID
        self.surveyID = surveyGen.generateID()
        #initialize questions with list of questions if one is provided
        #otherwise initialize questions to empty list
        self.questions = questions
        self.blockList = blocklist
        self.hasBreakoff = breakoff
        
    def addQuestion(self, question):
        #add question to end of survey
        self.questions.append(question)
        
    def addQuestionByIndex(self, question, index):
        #add question at certain index
        self.questions.insert(index, question)
        
    def removeQuestionByID(self, qid):
        #remove question from survey by its id
        for i in range(len(self.questions)):
            if self.questions[i].qpid==qpid:
                self.questions.pop(i)
                return
        
    def removeQuestionByIndex(self, index):
        #remove question from survey by its index
        self.questions.pop(index)
        
    def getQuestionByID(self, qid):
        #get question from survey by its id
        for q in self.questions:
            if q.qid==qid:
                return q
        print "No questions with given ID"
        
    def getQuestionByIndex(self, index):
        #get question from survey by its index:
        if index < len(self.questions):
            return self.questions[index]
        else:
            print "No question at index "+str(index)

    def randomize(self):
        #randomize blocks and questions, not sure how this works yet
        pass

    def __repr__(self):
        text = "Survey ID: "+self.surveyID + "\n"
        for q in self.questions:
            text = text + "\t" + str(q)+"\n"
        return text
        
    def __str__(self):
        #prints/returns string representation of current survey
        #include some visualization of current branch/block structure?
        text = "Survey ID: "+self.surveyID + "\n"
        for q in self.questions:
            text = text + str(q)
        return text
        
    def jsonize(self):
        #call jsonize on the questions in the question list
        #not sure what this entails
        pass

class Question:

    def __init__(self, qtype, qtext, options = [], shuffle=True):
        #initialize variables depending on how many arguments provided
        #call generateID
        self.qid = qGen.generateID()
        self.qtype = qtype
        self.qtext = qtext
        self.options = options
        self.shuffle = shuffle
        #self.blockid
        #self.branchid #list of qids the question branches to?

    def randomize(self):
        #is this the same as shuffling?
        pass

    def addOption(self, option):
        #add option to end of oplist
        self.options.append(option)

    def addOptionByIndex(self, index, option):
        #add option at certain index
        self.options.insert(index, option)
        
    def removeOptionByID(self, opid):
        #remove option from question by its id
        for i in range(len(self.options)):
            if self.options[i].opid==opid:
                self.options.pop(i)
                return
        
    def removeOptionByIndex(self, index):
        #remove option from question by its index
        self.options.pop(index)
        
    def getOptionByID(self, opid):
        #get option from question by its id
        for op in self.options:
            if op.opid==opid:
                return op
        print "No options with given ID"
        
    def getOptionByIndex(self, index):
        #get option from question by its index:
        if index < len(self.options):
            return self.options[index]
        else:
            print "No option at index "+str(index)

    def before(self, question2):
        #determines if question is before another question in a block
        #not sure what this is for, saw it in the java
        pass

    def __repr__(self):
        #print out question text and options
        text = "Question ID: "+str(self.qid)+" Question type: "+self.qtype+"\n"
        text = text + self.qtext + "\n"
        for o in self.options:
            text = text + "\t" + str(o) + "\n"
        return text
        
    def __str__(self):
        text = "Question ID: "+str(self.qid)+" Question type: "+self.qtype+"\n"
        text = text + self.qtext + "\n"
        for o in self.options:
            text = text + "\t" + str(o) + "\n"
        return text

    def jsonize(self):
        pass
        

class Option:
    
    def __init__(self, opText):
        #initialize option text field
        self.opText=opText
        #generate id for option
        self.opid=opGen.generateID()

    def jsonize(self):
        #not sure what this entails
        return
        
    def __repr__(self):
        return self.opText
        
    def __str__(self):
        return self.opText

##class Block:
##    #not sure how to specify this yet

def main():
    #testing option creation
    op1 = Option("this is an option");
    op2 = Option("this is another option");
    print op1.opid
    print op1

    print op2.opid
    print op2

    oplist = [op1, op2]
    print ""

    #testing quesion creation and methods
    q1 = Question("radio", "Question 1", oplist)
    print q1.qtext
    print q1.options
    q1.addOption(Option("third option"))
    print q1.options
    print q1.getOptionByID("op3").opText
    print q1.getOptionByIndex(2).opText
    q1.addOptionByIndex(1, Option("fourth option"))
    print q1.options
    print q1.options[1].opid
    q1.removeOptionByID("op4")
    q1.removeOptionByIndex(0)
    print q1.options

    print str(q1)

    survey1 = Survey([q1])
    print str(survey1)
    
if  __name__ =='__main__':
    main()


    
