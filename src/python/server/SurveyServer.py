import SimpleHTTPServer
import SocketServer
import cgi

class ServerHandler(SimpleHTTPServer.SimpleHTTPRequestHandler):

    # java should spit out HTML to a directory visible to the python server
    # (we do this for mturk already)
    # the challenge here is that we don't want to start serving pages until we 
    # get the signal that the page is ready from Java -> for now, let's manually
    # start up the thing after generating the preview
    
    # server should keep a counter for the number of users who have submitted
    # results so far and post a different page when the survey is complete
    # (not mission-critical)
    
    # for each form submit, the server should put the data in an xml format
    # that mimics mturk
    # the server should handle http requests that ask for results and return them as xml

    # FieldStorage(None, None, [MiniFieldStorage('sourceFilename', '/Users/etosch/dev/SurveyMan/data/SMLF5.csv'), MiniFieldStorage('resultFilename', '/Users/etosch/dev/SurveyMan/data/SMLF5.csv'), MiniFieldStorage('q_7_2', '{"quid":"q_7_2","oid":"comp_7_3","qpos":1,"opos":0}'), MiniFieldStorage('q_2_2', '{"quid":"q_2_2","oid":"comp_2_3","qpos":0,"opos":0}')])

    responses = []

    def do_GET(self):
        print SimpleHTTPServer.SimpleHTTPRequestHandler.do_GET(self)

    def do_POST(self):
        form = cgi.FieldStorage(
            fp=self.rfile,
            headers=self.headers,
            environ={'REQUEST_METHOD':'POST',
                     'CONTENT_TYPE':self.headers['Content-Type']})
        self.responses.push(form)
        print form
        print SimpleHTTPServer.SimpleHTTPRequestHandler.do_GET(self)



if __name__=="__main__":
    Handler = ServerHandler

    httpd = SocketServer.TCPServer(("", 8000), Handler)
    httpd.serve_forever()
