#requires Python 2.7.5
#attempt at survey representation
from survey_exceptions import *

qtypes = {"freetext" : 0 , "radio" : 1 , "check" : 2 , "dropdown" : 3}


#generate ids for survey components based on prefixes passed as arg
#op=option id, s=survey id, q=question id, b=block id, c=constraint id
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
blockGen = idGenerator("b")
constraintGen = idGenerator("c")

class Survey:

    def __init__(self, blocklist, constraints, breakoff = True):
        #generate ID
        self.surveyID = surveyGen.generateID()
        #survey is a list of blocks, which hold questions and subblocks
        #at least one block with all the questions in it
        self.blockList = blocklist
        #list of branching constraints
        self.constraints = constraints
        self.hasBreakoff = breakoff
        
    def addBlock(self, block):
        #add block to end of survey (assumed to be a top level block)
        self.blockList.append(block)
        
    def addBlockByIndex(self, block, index):
        #add block at certain index
        #throws index out of bounds exception (?)
        self.blockList.insert(index, block)


    def validate(self):
        #check if blocks contain either all or one branch questions
        for b in self.blockList:
            b.validBranchNumber(); #should throw exception if invalid
                
        #check that all branches branch to blocks in the survey
        for c in self.constraints:
            for bid in c.getBlocks():
                surveyHasBlock = False
                for b in self.blockList:
                    if b.blockid == bid:
                        surveyHasBlock = True
            if surveyHasBlock!=True:
            #throw InvalidBranchException
                badBranch = InvalidBranchException("Question "+c.question+" does not branch to a block in survey")
                raise badBranch()
        #check that all branches branch forward (not implemented yet)
        
    def __str__(self):
        #prints/returns string representation of current survey
        #include some visualization of current branch/block structure?
        output = "Survey ID: "+self.surveyID+"\n"
        for b in self.blockList:
            output = output+str(b)+"\n"
        return output
        
    def jsonize(self):
        self.validate()
        output = "{'breakoff' : '%s', 'survey' : [%s] }" %(self.hasBreakoff, ",".join([b.jsonize() for b in self.blockList]))
        output = output.replace("\'", "\"")
        return output
    
class Question:

    def __init__(self, qtype, qtext, options, block="none", shuffle=True, branching = False):
        #initialize variables depending on how many arguments provided
        #if you don't want to add options immediately, add empty list as argument
        #call generateID
        self.qid = qGen.generateID()
        self.qtype = qtype
        self.qtext = qtext
        self.options = options
        self.shuffle = shuffle
        self.branching = branching
        #self.blockid
        #self.branchid #list of blocks the question branches to?

    def addOption(self, oText):
        #add option to end of oplist
        #pass op text as argument
        o = Option(oText)
        self.options.append(o)

    def addOptionByIndex(self, index, otext):
        #add option at certain index
        o = Option(oText)
        self.options.insert(index, o)


    def before(self, question2):
        #determines if question is before another question in a block
        #not sure what this is for, saw it in the java
        pass

    def equals(self, q2):
        return self.qid==q2.qid
        
    def __str__(self):
        text = "Question ID: "+str(self.qid)+" Question type: "+self.qtype+"\n"
        text = text + self.qtext + "\n"
        for o in self.options:
            text = text + "\t" + str(o) + "\n"
        return text

    def jsonize(self):
        #call validate to check if survey is valid
        if hasattr(self, "branchMap"):
            output = "{'id' : '%s', 'qtext' : '%s', 'options' : [%s], 'branchMap' : %s}"%(self.qid, self.qtext, ",".join([o.jsonize() for o in self.options]), self.branchMap.jsonize())
        else:   
            output = "{'id' : '%s', 'qtext' : '%s', 'options' : [%s]}"%(self.qid, self.qtext, ",".join([o.jsonize() for o in self.options]))
        output = output.replace('\'', '\"');
        return output

class Option:
    
    def __init__(self, opText):
        #initialize option text field
        self.opText=opText
        #generate id for option
        self.opid=opGen.generateID()

    def equals(self, o2):
        return self.opid==o2.opid

    def jsonize(self):
        output = "{'id' : '%s', 'otext' : '%s' }" %(self.opid, self.opText)
        output = output.replace('\'', '\"');
        return output
        
    def __str__(self):
        return self.opText

class Block:

    def subblockIDs(self):
        #check if block contains other blocks, give them appropriate labels
        if(len(self.contents) != 0):
            for b in self.contents:
                if(isinstance(b,Block)):
                    b.blockid=self.blockid+(".")+b.blockid

    def __init__(self, contents, parent = "none", randomize = False):
        self.contents = contents #could contain blocks or questions
        self.blockid = blockGen.generateID()
        self.randomize = randomize
        self.subblockIDs()

    def addQuestion(self, question):
        question.block=self.blockid
        self.contents.append(question)
        
    def addSubblock(self, subblock):
        subblock.parent=self.blockid
        subblock.blockid = self.blockid+"."+subblock.blockid
        self.contents.append(subblock)

    def getSubblocks(self):
        subblocks = []
        for c in self.contents:
            if(isinstance(c,Block)):
                subblocks.append(c)
        return subblocks

    def getQuestions(self):
        questions = []
        for c in self.contents:
            if(isinstance(c,Question)):
                questions.append(c)
        return questions

    def validBranchNumber(self):
        branching = []
        numQuestions = len(self.getQuestions())
        for q in self.getQuestions():
            if q.branching == True:
                branching.append(q);
        
        if len(branching) !=0 and len(branching) !=1 and len(branching)!=numQuestions:
            #throw invalid branch exception
            print("should throw exception")
            badBranch = InvalidBranchException("Block contains too many branch questions")
            raise badBranch()
        
        if len(branching)!=0 and hasattr(branching[0], "branchMap"):
            blocksBranchedTo = branching[0].branchMap.getBlocks()
        for q in branching:
            if hasattr(q, "branchMap"):
                if q.branchMap.getBlocks() != blocksBranchedTo:
                    badBranch = InvalidBranchException("Block branches to different destinations")
                    raise badBranch()

    def equals(self, block2):
        return self.blockid == block2.blockid

    def __str__(self):
        output = "Block ID: "+self.blockid+"\n"
        for c in self.contents:
            output=output+str(c)+"\n"
        return output
    
    def jsonize(self):
        qs=[]
        bs=[]
        for q in self.contents:
            if(isinstance(q, Question)): 
                qs.append(q.jsonize())
            else:
                bs.append(q.jsonize())
        output = "{'id' : '%s', 'questions' : [%s], 'randomize' : '%s', 'subblocks' : [%s] }"%(self.blockid, ",".join(qs), self.randomize, ",".join(bs))
        output = output.replace('\'', '\"');
        return output

class Constraint:
    #defines a mapping from a question options to Blocks
    def __init__(self, question):
        self.cid = constraintGen.generateID()
        #add check here to make sure survey contains question
        question.branching = True
        question.branchMap = self
        self.question = question
        #holds list of tuples (opid, blockid)
        self.constraintMap = []
        for o in self.question.options:
            self.constraintMap.append((o.opid, "null"))

    def addBranchByIndex(self, opIndex, block):
        self.constraintMap[opIndex] =(self.question.options[opIndex].opid, block.blockid)
        #throws index out of bounds exception

    def addBranch(self, op, block):
        for i in range(len(self.question.options)):
            if self.question.options[i].equals(op):
                self.constraintMap[i] = (op.opid, block.blockid)
                return
        #print "question does not contain option "+opID
        noOp = NoSuchOptionException("Question "+self.question.quid+" does not contain option "+opID)
        raise noOp()

    def addBranchByOpText(self, opText, block):
        for i in range(len(self.question.options)):
            if self.question.options[i].opText==opText:
                self.constraintMap[i] = (self.question.options[i].opid, block.blockid)
                return
        #print "question does not contain option "+opID
        noOp = NoSuchOptionException("Question "+self.question.quid+" does not contain option \""+opText+'\"')
        raise noOp()

    #returns all blocks branched to by this question
    def getBlocks(self):
        output = []
        for c in self.constraintMap:
            output.append(c[1])
        return output

    def __str__(self):
        output = "Constraint ID: "+self.cid+"\n"+"branches: \n"
        for (opid, blockID) in self.constraintMap:
            output = output+"\t"+str((opid, blockID))+"\n"
        return output

    def jsonize(self):
        temp = "";
        cmap = []
        for tup in self.constraintMap:
            temp+="'"+tup[0]+"' : "
            if(tup[1] == "null"):
                temp+="null"
            else:
                temp+="'"+tup[1]+"'"
            cmap.append(temp)
            temp=""
        output = "[%s]"%(",".join(cmap))
        output = output.replace('\'', '\"');
        output= output.replace('[','{')
        output = output.replace(']','}')
        return output
        
        
def main():
    pass
    
    
if  __name__ =='__main__':
    main()


    
