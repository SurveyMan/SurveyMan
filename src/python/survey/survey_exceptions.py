#survey exception class
#based on suggestions from
#http://stackoverflow.com/questions/1319615/proper-way-to-declare-custom-exceptions-in-modern-python

class ExceptionTemplate(Exception):
    def __call__(self, *args):
        return self.__class__(*(self.args + args))

class SurveyException(ExceptionTemplate):
    pass

class InvalidBlockException(ExceptionTemplate):
    pass

class InvalidBranchException(ExceptionTemplate):
    pass
#may be uneccesary if remove/get methods are deleted
##class NoSuchBlockException(ExceptionTemplate):
##    pass
##
##class NoSuchQuestionException(ExceptionTemplate):
##    pass
##
##class NoSuchOptionException(ExceptionTemplate):
##    pass

def main():
    survey_exception = SurveyException("survey exception raised");
    raise survey_exception();

if __name__=='__main__':
    main()
