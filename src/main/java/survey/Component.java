package survey;

import utils.Either;
import java.lang.RuntimeException;
import java.net.URL;

class Component extends Either<String, URL> {

    // A component is one of the elements of a survey.
    // Components get unique ids
    // Components may be text or a url
    // Their layout may be controlled by an input css file

    private static final Gensym gensym = new Gensym("comp");
    public final String uid = gensym.next();

    private Either<String, URL> data;
    private Class ctype;
    public String cname = "";

    Component(String ctext) {
        this.data = Left<String, URL>( ctext );
        this.ctype = java.lang.String;
    }

    Component(String ctext, String cname) {
        this.cname = cname;
        Component(ctext);
    }

    Component(URL curl) {
        this.data = Right<String, URL>( curl );
        this.ctype = java.net.URL;
    }

    Component(URL curl, String cname) {
        this.cname = cname;
        Component(curl);
    }

    public Object getData() throws RuntimeException {
        return (ctype) data.get();
    }

    public static void main(String[] args){
        // write test code here
    }


}