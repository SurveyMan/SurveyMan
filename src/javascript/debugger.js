var sm = {},
    corrs = {},
    bkoffs = {},
    variants = {},
    order = {},
    responses = {},
    surveyData = [],
    resultsData = [],
    staticCurrentSurveyId = "staticCurrentSurvey",
    dynamicCurrentSurveyId = "dynamicCurrentSurvey";
    staticBtnCurrentSurveyId = "staticBtnCurrentSurvey",
    dynamicBtnCurrentSurveyId = "dynamicBtnCurrentSurvey",
    targets = ["overview", "static", "dynamic"],
    margin = { top : 20, right : 0, bottom : 0, left: 15},
    width = 960,
    height = width;

var toggle_task         =   function (target) {

        console.log(target);

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
    display_correlations = function () {
        return makeHeatmap(corrs, sm);
    },
    display_breakoff    = function () {
        return makeBarchart(bkoffs, sm);
    },
    display_variants    = function () {
        return makeVariantDisplay(variants, sm);
    },
    display_order       = function () {
        return makeOrderDisplay(order, sm);
    },
    display_scores      = function () {
        return makeScoresDisplay(responses, sm);
    },
    analysis            =   function (reportType, csv, local, data) {

            var report  =   reportType ? "static" : "dynamic",
                obj     =   {"report" : report,
                             "survey" : csv,
                             "local" : (local || false),
                             "surveyData" : (data ? data["surveyData"] : ""),
                             "resultsData" : (data ? data["resultsData"] : "")
                             };
            console.log(obj);
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
                    sm = new SurveyMan(retval['sm']);
                    corrs = retval['corrs'];
                    bkoffs = retval['bkoffs'];
                    variants = retval['variants'];
                    order = retval['order'];
                    responses = retval['responses'];
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

            var report              =   reportType ? "static" : "dynamic",
                currentSurvey       =   reportType ? $("#"+staticCurrentSurveyId) : $("#"+dynamicCurrentSurveyId),
                btnCurrentSurvey    =   reportType ? $("#"+staticBtnCurrentSurveyId) : $("#"+dynamicBtnCurrentSurveyId);

            console.log(arguments);

            $(btnCurrentSurvey).html(displayText);
            $(btnCurrentSurvey).unbind("click");
            $(btnCurrentSurvey).click((function (cbk, btnCurrentSurvey) {
                return function() {
                        cbk();
                        $(btnCurrentSurvey).button('loading');
                     };
                })(cbk, btnCurrentSurvey));

            if (!local) {
                btnCurrentSurvey.removeAttr("disabled");
                $("#dynamicBtnResults").attr("disabled", "disabled");
            }

            if (results) {
                $("#dynamicBtnCurrentSurvey").html($("#dynamicBtnCurrentSurvey").html()+"<br/>"+displayText);
                $("#dynamicBtnCurrentSurvey").removeAttr("disabled");
            } else {
                $("#dynamicCurrentResults").hide();
                $(currentSurvey).show();
                if (reportType){
                    $("#staticData").empty();
                } else {
                    $("#heatmap").empty();
                    $("#questionCloseup").empty();
                }

            }

        },
    handleFileSelect    = function (evt, reportType, results) {

            if (!(window.File || window.FileReader || window.FileList || window.Blob)) {
                alert("Cannot upload files! The File APIs are not fully supported on your browser");
            } else {

                var files = evt.target.files; // FileList object

                // files is a FileList of File objects. List some properties.
                var display = [];
                console.assert(files.length === 1);
                f = files[0];
                display.push('<strong>', escape(f.name), '</strong> (', f.type || 'n/a', ') - ',
                              f.size, ' bytes, last modified: ',
                              f.lastModifiedDate ? f.lastModifiedDate.toLocaleDateString() : 'n/a',
                              '');

                //read the data from the file into memory
                var r = new FileReader();

                r.onload = (function (selectedFile, display, reportType, results) {
                    return function (evt) {
                        //return the string data
                        var data = evt.target.result,
                            cbk  = function() {
                                       analysis(reportType, selectedFile.name, true, {"surveyData" : surveyData, "resultsData" : resultsData});
                                    };
                        if (results) {
                            // then it must be dynamic analyses
                            resultsData = data;
                        } else {
                            surveyData = data;
                        }
                        console.log(cbk);
                        updateCurrentSurvey(display, f.name, reportType, true, results, cbk);
                    }
                })(f, display.join(''), reportType, results);

                r.readAsText(f,"UTF-8");
            }
        },
    sendLocalSurvey   =   function(){
        console.assert($("#dynamicBtnCurrentResults").attr("disabled"));
        };


var NaNOrZero = function (_d) {
                    if (isNaN(_d.corr))
                        return 0;
                    else return _d.corr;
                };

