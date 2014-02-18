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
blockGen = idGenerator("b")

class Survey:

    def __init__(self, blocklist = [], breakoff = True):
        #generate ID
        self.surveyID = surveyGen.generateID()
        #survey is a list of blocks, which hold questions
        #at least one block with all the questions in it
        self.blockList = blocklist
        self.hasBreakoff = breakoff
        #add branching later
        
    def addBlock(self, block):
        #add block to end of survey
        self.blockList.append(block)
        
    def addBlockByIndex(self, block, index):
        #add question at certain index
        self.blockList.insert(index, block)
        
    def removeBlockByID(self, blockid):
        #remove block from survey by its id
        #if lowest subblock specified, remove sublock
        #else remove block and all its subblocks
        for i in range(len(self.blockList)):
            if self.blockList[i].blockid==blockid:
                self.blockList.pop(i)
                return
            elif blockid.startswith(self.blockList[i].blockid):
                for subB in self.blockList[i]:
                    if(subB.blockid==blockid):
                        self.blockList[i].pop(subB)
                        return
    def getBlockByID(self, blockid):
        #get block from survey by its id
        for i in range(len(self.blockList)):
            if self.blockList[i].blockid==blockid:
                return self.blockList[i]
            elif blockid.startswith(self.blockList[i].blockid):
                for subB in self.blockList[i]:
                    if(subB.blockid==blockid):
                        return self.blockList[i][subB]
        
    def randomize(self):
        #randomize blocks and questions, not sure how this works yet
        pass

    def __repr__(self):
        text = "Survey ID: "+self.surveyID + "\n"
        for b in self.block:
            text = text + "\t" + str(b)+"\n"
        return text
        
    def __str__(self):
        #prints/returns string representation of current survey
        #include some visualization of current branch/block structure?
        output = "Survey ID: "+self.surveyID+"\n"
        for b in self.blockList:
            output = output+str(b)+"\n"
        return output
        
    def jsonize(self):
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
         return "id : "+self.qid+" qtext : "+self.qtext+" options : "+[o.jsonize() for o in self.options]
         
        

class Option:
    
    def __init__(self, opText):
        #initialize option text field
        self.opText=opText
        #generate id for option
        self.opid=opGen.generateID()

    def jsonize(self):
        return "id : "+ self.oid+ " otext : " + self.otext
        
    def __repr__(self):
        return self.opText
        
    def __str__(self):
        return self.opText

class Block:

    def subblockIDs(self):
        #check if block contains other blocks, give them appropriate labels
        if(len(self.contents) != 0):
            for b in self.contents:
                if(isinstance(b,Block)):
                    b.blockid=self.blockid+(".")+b.blockid

    def __init__(self, contents = []):
        self.contents = contents #could contain blocks or questions
        self.blockid = blockGen.generateID()
        self.subblockIDs()

    def addQuestion(self, question):
        self.contents.append(question)
        
    def removeQuestion(self, qid):
        #remove question by qid
        for i in range(len(self.contents)):
            if(isinstance(self.contents[i],Question) and self.contents[i].qid == qid):
                self.contents.pop(i)
                return
        print "Question "+qid+" is not in block "+self.blockid
        
    def addSubblock(self, subblock):
        self.contents.append(subblock)

    def removeSubblock(self, blockid):
        for i in range(len(self.contents)):
            if(isinstance(self.contents[i].blockid, Block) and self.contents[i].blockid == blockid):
                self.contents.pop(i)
                return
        print "Block "+self.blockid+" does not contain "+blockid

    def __str__(self):
        output = self.blockid+"\n"
        for c in self.contents:
            output=output+str(c)+"\n"
        return output

    def __repr__(self):
        output = self.blockid+"\n"
        for c in self.contents:
            output=output+str(c)+"\n"
        return output
    
    def jsonize(self):
        pass
        
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

    q2=Question("radio", "Question 2", oplist)

    questions = [q1]

    #testing block creation and methods
    block1 = Block(questions)
    block1.addQuestion(q2)
    print str(block1)

    block2 = Block([block1])
    print str(block2)
    print "removing question 1"
    block1.removeQuestion("q1")
    block2.removeQuestion("q1")
    print str(block1)
    print "removing block 1 from block2"
    block2.removeSubblock("b2.b1")
    print str(block2)

    print "\n"
    block3 = Block()
    survey = Survey()
    survey.addBlock(block2)
    survey.addBlockByIndex(block3,0)
    print str(survey)
    print survey.getBlockByID("b2")
    survey.removeBlockByID("b2")
    
    print str(survey)
    
    
if  __name__ =='__main__':
    main()


    
