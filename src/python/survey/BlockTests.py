import unittest
import survey_representation
import survey_exceptions
import subblock_example
import example_survey
import SimpleSurvey


class BlockTests(unittest.TestCase):

    def setUp(self):
        self.blockSurvey = subblock_example.createSurvey()
        self.ipierotisSurvey = example_survey.createSurvey()
        self.simpleSurvey = SimpleSurvey.createSurvey()

    def tearDown(self):
        del self.blockSurvey
        del self.ipierotisSurvey
        del self.simpleSurvey

    def countBlocks(self,blockList):
        if(len(blockList)==0):
            return 0
        else:
            sumSubblocks = 0
            for b in blockList:
                sumSubblocks += self.countBlocks(b.getSubblocks())
            return sumSubblocks + len(blockList)
                

    #assert that the test surveys contain the desired number of blocks
    def testBlockNumber(self):
        print("Running block number test")
        self.assertEqual(self.countBlocks(self.ipierotisSurvey.blockList),3)
        self.assertEqual(self.countBlocks(self.blockSurvey.blockList),9)
        self.assertEqual(self.countBlocks(self.simpleSurvey.blockList),3)
     
        

    #just testing that the tests work
    def test(self):
        print("test")
        self.assertEqual(True,True)
    

if __name__=='__main__':
    unittest.main()
