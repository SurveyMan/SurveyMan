var sm, local, staticCurrentSurvey, dynamicCurrentSurvey;
var targets = ["overview", "static", "dynamic"];

var toggle_task         =   function (target) {
            //console.log("target " + target);
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
    analysis            =   function (reportType, csv, local, f, evt) {
            var data = "";
            if (local && !(window.File || window.FileReader || window.FileList || window.Blob)) {
                alert("Cannot upload files! The File APIs are not fully supported on your browser");
            } else if (local) {
                var r = new FileReader();
                r.onload = function (evt) {
                        console.log("filereader results" + r.results);
                    };
                data = r.readAsText(f);
                console.log("data: " + data);
            }

            var report  =   reportType ? "static" : "dynamic",
                obj     =   {"report" : report,
                             "survey" : csv,
                             "local" : local,
                             "data" : data
                             };
            console.log(obj);
            if (reportType) {
                $.get("", obj, function (s) {
                    console.log(s);
                    $("#" + report + "Data").html("<br/>" + s + "<br/>");
                });
            } else {
                $.get("sm", obj, function (s) { sm = s; }); // produces the csvs
                console.log(sm);
            }
        },
    updateCurrentSurvey = function(display, filename, reportType, local, f, evt) {
            // reportType=true is static
            var currentSurvey       =   document.getElementById('currentSurvey'),
                btnCurrentSurvey    =   $("#btnCurrentSurvey");
            $(btnCurrentSurvey).html(display);
            $(btnCurrentSurvey).unbind("click");
            $(btnCurrentSurvey).click(function () {
                analysis(reportType, filename, local, f, evt);
                });
            $(currentSurvey).show();
            $("#" + (reportType ? "static" : "dynamic") + "Data").empty();
        },
    handleFileSelect    = function (evt, reportType) {
            var files = evt.target.files; // FileList object

            // files is a FileList of File objects. List some properties.
            var output = [];
            console.assert(files.length === 1);
            f = files[0];
            output.push('<strong>', escape(f.name), '</strong> (', f.type || 'n/a', ') - ',
                          f.size, ' bytes, last modified: ',
                          f.lastModifiedDate ? f.lastModifiedDate.toLocaleDateString() : 'n/a',
                          '');
            updateCurrentSurvey(output.join(''), f.name, reportType, true, f, evt);
        };

document.getElementById('staticFiles').addEventListener('change', function (evt) { handleFileSelect(evt, true); }, false);
$("#currentSurvey").addEventListener('click', function (evt) {
