#attempt at survey representation

class Survey:
    def surveyID
    def questionList 
    def blockList
    def hasBreakoff

    def __init__(self, *args):
        #initialize questions with list of questions if one is provided
        #otherwise initialize questions to empty list
        #call generate ID

    def generateID():
        #generates a unique id for the survey
        
    def addQuestion(question):
        #add question to survey
        #if no index specified, add to end
        
    def addQuestion(question, index):
        #add question at certain index
        
    def removeQuestionByID(qid):
        #remove question from survey by its id
        
    def removeQuestionByIndex(index):
        #remove question from survey by its index
        
    def getQuestionByID(qid):
        #get question from survey by its id
        
    def getQuestionByIndex(index):
        #get question from survey by its index:

    def randomize:
        #randomize blocks and questions, not sure how this works yet

    def __repr__(self):
        
    def __str__(self):
        #prints/returns string representation of current survey
        #include some visualization of current branch/block structure?
        
    def jsonize(self):
        #call jsonize on the questions in the question list
        #not sure what this entails

class Question:
    def qid
    def blockid
    def branchid #list of qids the question branches to?
    def qtype #predefined types - "RADIO, DROPDOWN, CHECKBOX"
    def qtext #question text
    def options #list of Option objects associated with question

    def __init__(self, *args):
        #initialize variables depending on how many arguments provided
        #call generateID

    def generateID():
        #generates a unique id for the question

    def randomize():
        #is this the same as shuffling?

    def addOption(option, index):
        #add question at certain index
        
    def removeOptionByID(opid):
        #remove option from question by its id
        
    def removeOptionByIndex(index):
        #remove option from question by its index
        
    def getOptionByID(qid):
        #get option from question by its id
        
    def getOptionByIndex(index):
        #get option from question by its index:

    def before(question2):
        #determines if question is before another question in a block
        #not sure what this is for, saw it in the java

    def equals(question2):
        #is equal to another question
        #not sure if applicable

    def __repr__(self):
        #print out question text and options
        
    def __str__(self):
        #print out question text and options

    def jsonize(self):
        #jsonize options
        

class Option:
    def opText
    def opid

    def __init__(self, opText)
        #initialize optText
        #call generateID()

    def generateID():
        #generates a unique id for the option

    def jsonize(self):
        #not sure what this entails

class Block:
    #not sure how to specify this yet



    
