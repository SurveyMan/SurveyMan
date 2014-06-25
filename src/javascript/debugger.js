var sm = null,
    staticCurrentSurveyId = "staticCurrentSurvey",
    dynamicCurrentSurveyId = "dynamicCurrentSurvey";
    staticBtnCurrentSurveyId = "staticBtnCurrentSurvey",
    dynamicBtnCurrentSurveyId = "dynamicBtnCurrentSurvey",
    targets = ["overview", "static", "dynamic"];

var toggle_task         =   function (target) {

            for ( var i = 0 ; i < targets.length ; i++ ) {
                if ( targets[i]===target && $("#"+targets[i]).is(":hidden") ){
                    //console.log("selected: " + targets[i]);
                    $('#' + target).toggle('show');
                    $('#' + target + '-li').addClass('active');
                } else if ( targets[i]!=target && ! $("#"+targets[i]).is(":hidden") ){
                    //console.log("unselected: " + targets[i]);
                    $('#' + targets[i]).toggle('hide');
                    $('#' + targets[i] + '-li').removeClass('active');
                }
            }

        },
    analysis            =   function (reportType, csv, local, data) {

            var report  =   reportType ? "static" : "dynamic",
                obj     =   {"report" : report,
                             "survey" : csv,
                             "local" : local,
                             "data" : data
                             };

            if (reportType) {
                $.post("", obj, function (s) {
                    console.log(s);
                    $("#" + report + "Data").html("<br/>" + s + "<br/>");
                });
            } else {
                $.get("sm", obj, function (s) { sm = s; }); // produces the csvs
                console.log(sm);
            }

        },
    updateCurrentSurvey = function(display, filename, reportType, local, data) {

            var report              =   reportType ? "static" : "dynamic",
                currentSurvey       =   reportType ? $("#"+staticCurrentSurveyId) : $("#"+dynamicCurrentSurveyId),
                btnCurrentSurvey    =   reportType ? $("#"+staticBtnCurrentSurveyId) : $("#"+dynamicBtnCurrentSurveyId);

            $(btnCurrentSurvey).html(display);
            $(btnCurrentSurvey).unbind("click");
            $(btnCurrentSurvey).click(function () {
                    analysis(reportType, filename, local, data);
                });
            $(currentSurvey).show();
            $("#" + report + "Data").empty();

        },
    handleFileSelect    = function (evt, reportType) {
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
                r.onload = (function (selectedFile, display, reportType) {
                    return function (evt) {
                        //return the string data
                        var data = evt.target.result;
                        updateCurrentSurvey(display.join(''), f.name, reportType, true, data);
                    }
                })(f, display, reportType);

                r.readAsText(f,"UTF-8");

            }
        };