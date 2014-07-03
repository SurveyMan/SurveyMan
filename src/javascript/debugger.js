var sm = {},
    corrs = {},
    bkoffs = {},
    surveyData = [],
    resultsData = [],
    staticCurrentSurveyId = "staticCurrentSurvey",
    dynamicCurrentSurveyId = "dynamicCurrentSurvey";
    staticBtnCurrentSurveyId = "staticBtnCurrentSurvey",
    dynamicBtnCurrentSurveyId = "dynamicBtnCurrentSurvey",
    targets = ["overview", "static", "dynamic"];

var toggle_task         =   function (target) {

            for ( var i = 0 ; i < targets.length ; i++ ) {
                if ( targets[i]===target && $("#"+targets[i]).is(":hidden") ){
                    $('#' + target).show();
                    $('#' + target + '-li').addClass('active');
                } else if ( targets[i]!=target && ! $("#"+targets[i]).is(":hidden") ){
                    $('#' + targets[i]).hide();
                    $('#' + targets[i] + '-li').removeClass('active');
                }
            }

        },
    display_correlations = function () {
            return makeHeatmap(corrs, sm);
    },
    display_breakoff    = function () {
        return makeBarchart(bkoffs, sm);
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
                    $("#correlation").removeClass("disabled");
                    $("#breakoff").removeClass("disabled");
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