var globals = {}
    surveyData = [],
    resultsData = [],
    staticCurrentSurveyId = "staticCurrentSurvey",
    dynamicCurrentSurveyId = "dynamicCurrentSurvey",
    staticBtnCurrentSurveyId = "staticBtnCurrentSurvey",
    dynamicBtnCurrentSurveyId = "dynamicBtnCurrentSurvey",
    targets = ["overview", "static", "dynamic"],
    margin = { top : 20, right : 0, bottom : 25, left: 25},
    width = 960 - margin.left - margin.right,
    height = width;

globals.sm = {},
globals.corrs = {},
globals.bkoffs = {},
globals.variants = {},
globals.order = {},
globals.responses = {};

var toggle_task         =   function (target) {

        $.get(target + ".html"
            , function (data) {
                $("#content").empty();
                $("#content").html(data);
                if (target==="static") {
                    document.getElementById('staticFiles').addEventListener('change', function (evt) { handleFileSelect(evt, true); }, false);
                } else if (target==="dynamic") {
                    document.getElementById('dynamicFiles').addEventListener('change', function (evt) { handleFileSelect(evt, false);}, false);
                    document.getElementById('dynamicBtnResults').addEventListener('change', function (evt) { handleFileSelect(evt, false, true);}, false);
                }
            }
        );

        },
    analysis            =   function (reportType, csv, local, data) {

            console.log(data);

            var report  =   reportType ? "static" : "dynamic",
                obj     =   {"report" : report,
                             "survey" : csv,
                             "local" : (local || false),
                             "surveyData" : (data ? data["surveyData"] : ""),
                             "resultsData" : (data ? data["resultsData"] : "")
                             };

            console.assert($.isPlainObject(obj));

            if (reportType) {
                $.post("", obj, function (s) {
                    console.log(s);
                    $("#" + report + "Data").html("<br/>" + s + "<br/>");
                }).always(function () {
                    $("#staticBtnCurrentSurvey").button("reset");
                });
            } else {
                $.post("", obj, function (s) {
                    console.log(s);
                    var retval = JSON.parse(s);
                    globals.sm = new SurveyMan(retval['sm']);
                    globals.corrs = retval['corrs'];
                    globals.bkoffs = retval['bkoffs'];
                    globals.variants = retval['variants'];
                    globals.order = retval['order'];
                    globals.responses = retval['responses'];
                    $("#correlation").removeClass("disabled");
                    $("#breakoff").removeClass("disabled");
                    $("#variants").removeClass("disabled");
                    $("#order").removeClass("disabled");
                    $("#scores").removeClass("disabled");
                }).always(function () {
                    $("#dynamicBtnCurrentSurvey").button("reset");
                }
                ); // produces the csvs
            }

        },
    updateCurrentSurvey = function(displayText, filename, reportType, local, results, cbk) {

            console.log(arguments);

            var currentResults      =   "#dynamicBtnResults",
                btnCurrentSurvey    =   reportType ? $("#"+staticBtnCurrentSurveyId) : $("#"+dynamicBtnCurrentSurveyId);

             // start -> dynamicBtnResults disabled, btnCurrentSurvey not visible
             // select from dropdown (!local) -> dynamicBtnResults disabled; btnCurrentSurvey visible && not disabled
             // select from upload your own (local && !results) -> dynamicBtnResults enabled; btnCurrentSurvey visible && disabled
             // select from upload your own results (local && results) -> dynamicBtnResults enabled; btnCurrentSurvey visible && enabled

            // after the first click, this will always be visible
            $(btnCurrentSurvey).css("visibility", "visible");

            // rebind clicking behavior every time
            $(btnCurrentSurvey).unbind("click");
            $(btnCurrentSurvey).click((function (cbk, btnCurrentSurvey) {
                return function() {
                        cbk();
                        $(btnCurrentSurvey).button('loading');
                     };
                })(cbk, btnCurrentSurvey));

            if (local && results) {
                // just clicked on button to upload results
                // there may be an old results file here
                var br = "<br>",
                    surveyname = $(btnCurrentSurvey).html().split(br)[0];
                $(btnCurrentSurvey).html(surveyname + br + displayText);
                $(btnCurrentSurvey).removeClass("disabled");
            } else if (local && !results) {
                // just clicked on button to upload survey
                $(currentResults).removeClass("disabled");
                $(btnCurrentSurvey).addClass("disabled");
                $(btnCurrentSurvey).html(displayText);
            } else if (!local) {
                $(currentResults).addClass("disabled");
                $(btnCurrentSurvey).html(displayText);
                $(btnCurrentSurvey).removeClass("disabled");
                surveyData = [];
                resultsData = [];
            }

            if (reportType)
                $("#staticData").empty();
        },
    handleFileSelect    = function (evt, reportType, results) {
            console.log("handling file select");

            if (!(window.File || window.FileReader || window.FileList || window.Blob)) {
                alert("Cannot upload files! The File APIs are not fully supported on your browser");
            } else {

                var files = evt.target.files; // FileList object
                console.log(files);

                // files is a FileList of File objects. List some properties.
                var display = [];
                console.assert(files.length === 1);
                f = files[0];
                display.push('<strong>', escape(f.name), '</strong> (', f.type || 'n/a', ') - ',
                              f.size, ' bytes, last modified: ',
                              f.lastModifiedDate ? f.lastModifiedDate.toLocaleDateString() : 'n/a',
                              '');

                              console.log(display);

                //read the data from the file into memory
                var r = new FileReader();

                r.onload = (function (selectedFile, display, reportType, results) {
                    return function (evt) {
                        //return the string data
                        if (results) {
                            // then it must be dynamic analyses
                            resultsData = data;
                        } else {
                            surveyData = data;
                            resultsData = [];
                        }
                        var data = evt.target.result,
                            cbk  = function() {
                                       analysis(reportType, selectedFile.name, true, {"surveyData" : surveyData, "resultsData" : resultsData});
                                    };
                        updateCurrentSurvey(display, f.name, reportType, true, results, cbk);
                    }
                })(f, display.join(''), reportType, results);

                r.readAsText(f,"UTF-8");
            }
        },
    sendLocalSurvey   =   function(){
        console.assert($("#dynamicBtnCurrentResults").attr("disabled"));
        };
