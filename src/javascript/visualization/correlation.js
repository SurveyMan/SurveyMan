var display_correlations = (function (globals) {

    return function() {

        var NaNOrZero = function (_d) {
                            if (isNaN(_d.corr))
                                return 0;
                            else return _d.corr;
                        };

        var makeCorrChart = function (d) {

            var height = 400,
                width = height,
                top = 150,
                bottom = 25,
                left = 25,
                right = 150,
                xInterval = width / d.q1.options.length,
                yInterval = height / d.q2.options.length,
                radius = 5,
                numCirclesAcross = Math.floor(xInterval / (2 * radius)),
                numCirclesDown = Math.floor(yInterval / (2 * radius)),
                data = _.filter(getJsonizedResponses()
                                , function (r) {
                                    return _.filter(r, function (qr) { return qr.q === d.q1.id; }).length === 1
                                        && _.filter(r, function (qr) { return qr.q === d.q2.id; }).length === 1;
                                }),
                getResponseOpt = function (q) {
                    return function(resp) {
                        return _.indexOf(_.pluck(q.options, 'id')
                                         , _.filter(resp, function (r) { return r.q === q.id; })[0].opts[0].o);
                    };
                };


            var svg = d3.selectAll("#respComp").append("svg")
                .attr("width", width +left + right)
                .attr("height", height + top + bottom)
                .attr("class", "center-block")
                    .append("g")
                    .attr("transform", "translate(" + left + "," + top + ")");
            
            // make pallet
            
            svg.append("rect")
                .attr("width", width)
                .attr("height", height)
                .attr("fill", "white")
                .attr("stroke", "black")
                .attr("stroke-width", 2);
        
            svg.append("text")
                .text(d.q1.qtext)
                .attr("x", 0)
                .attr("y", height + 20);
                
            svg.append("text")
                .text(d.q2.qtext)
                .attr("transform", "translate(-20," + height + ")")
                .style("text-anchor", "end")
                .attr("transform", "rotate(-90)");
                
            svg.selectAll("xlines")
                .data(_.rest(d.q1.options)).enter()
                .append("line")
                .attr("x1", function (d, i) { return i * xInterval; })
                .attr("x2", function (d, i) { return i * xInterval; })
                .attr("y1", -5)
                .attr("y2", height)
                .attr("stroke", "black")
                .attr("stroke-width", 1);
                
             svg.selectAll("ylines")
                .data(_.rest(d.q1.options)).enter()
                .append("line")
                .attr("y1", function (d, i) { return i * yInterval; })
                .attr("y2", function (d, i) { return i * yInterval; })
                .attr("x1", 0)
                .attr("x2", 5 + width)
                .attr("stroke", "black")
                .attr("stroke-width", 1);


        };

        var getCorrelationData = function (d) {
            var div = $.parseHTML("<div class='col-md-12' id='respComp'>"
                +"<p><b>#/Responses</b>:&nbsp;"+d.ct1
                +"&nbsp;&nbsp;<b>Coeff</b>:&nbsp;"+d.coeff
                +"&nbsp;&nbsp;<b>Val</b>:&nbsp;"+d.corr
                +"</p>"+"</div>");
            $(div).css("margin-top" , "10px");
            return div;
        };

        var zoomCorrelation = function(d) {
            // hide heatmap
            $("#corrs").hide();
            $("#questionCloseup").empty();
            $("#questionCloseup").append(getCorrelationData(d));
            $("#questionCloseup").append(getQuestionHTML(d.q1));
            $("#questionCloseup").append(getQuestionHTML(d.q2));
            $("#questionCloseup").append("<div class=\"col-md-10\"><button class=\"btn\" onclick='$(\"#questionCloseup\").empty(); $(\"#corrs\").show();'>Return to Heatmap</button></div>");
            makeCorrChart(d);
            $("#questionCloseup").show();
        };

        var questions = _.map(_.filter(globals.sm.survey.questions
                                       , function(_q) { return !_q.freetext;})
                              , function (_q) { return _q.id; }),
                gridSize = Math.floor(width / questions.length),
                legendElementWidth = width/9,
                legendElementHeight = gridSize*2,
                buckets = 9,
                //colors = ["#ffffd9","#edf8b1","#c7e9b4","#7fcdbb","#41b6c4","#1d91c0","#225ea8","#253494","#081d58"],
                colors = ['rgb(178,24,43)','rgb(214,96,77)','rgb(244,165,130)','rgb(253,219,199)','rgb(247,247,247)','rgb(209,229,240)','rgb(146,197,222)','rgb(67,147,195)','rgb(33,102,172)'],
                threshholds = [-0.8, -0.6, -0.4, -0.2, 0, 0.2, 0.4, 0.6, 0.8],
                legendLabels = ["&lt; -0.77", "-0.77 &mdash; -0.55", "-0.55 &mdash; -0.33"
                                , "-0.33 &mdash; -0.11", "-0.11 &mdash; 0.11", "0.11 &mdash; 0.33"
                                , "0.33 &mdash; 0.55", "0.55 &mdash; 0.77", "&gt; 0.77"],
                processCorrs = function (_d) {
                        // this gets called on
                        return {
                            q1 : globals.sm.getQuestionById(_d.q1),
                            ct1 : +_d.ct1,
                            q2 : globals.sm.getQuestionById(_d.q2),
                            ct2 : +_d.ct2,
                            corr : parseFloat(_d.val),
                            coeff : _d.coeff,
                            expected : JSON.parse(_d.expected)
                        };
                    },
                colorScale = d3.scale.quantile()
                                  .domain([-1.0, 1.0])
                                  .range(colors);

        var svg = d3.select("#corrs").append("svg")
                                  .attr("width", width + margin.left + margin.right)
                                  .attr("height", height + margin.top + margin.bottom)
                                  .append("g")
                                  .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

        var heatMap = svg.selectAll("#corrs")
                        .data(_.map(globals.corrs, processCorrs))
                        .enter()
                        .append("rect")
                        .attr("x", function(d) { return _.indexOf(questions, d.q1.id) * gridSize; })
                        .attr("y", function(d) { return _.indexOf(questions, d.q2.id) * gridSize; })
                        .attr("rx", 4)
                        .attr("ry", 4)
                        .attr("class", "hour bordered")
                        .attr("width", gridSize)
                        .attr("height", gridSize)
                        .style("fill", function (d) { return colorScale(d.corr); });


        heatMap.attr("cursor", "pointer")
            .on("click", function (d, i) {
                    zoomCorrelation(d);
                    return;
                });

        heatMap.append("title").text(function(d) { return d.q1.qtext + "\n" + d.q2.qtext + "\nval: " + d.corr; });

        var legend = svg.selectAll(".legend")
            .data(threshholds)
            .enter().append("g")
            .attr("class", "legend");

        legend.append("rect")
          .attr("x", function(d) { return legendElementWidth * _.indexOf(threshholds, d); })
          .attr("y", globals.sm.survey.questions.length*gridSize)
          .attr("width", legendElementWidth)
          .attr("height", legendElementHeight)
          .style("fill", function(d) { return colors[_.indexOf(threshholds, d)]; });

        legend.append("text")
          .html(function (d) { return legendLabels[_.indexOf(threshholds, d)]; })
          .attr("font-weight", "bold")
          .attr("color", "black")
          .attr("font-size", "small")
          .attr("x", function(d) { return (legendElementWidth * _.indexOf(threshholds, d)) + 10; })
          .attr("y", questions.length*gridSize + 15);

        var expected_show = d3.select("#unexpectedCorrs");
        var expected_hide = d3.select("#allCorrs");
        var only_expected = d3.select("#failedCorrs");

        expected_show.on("click", function () {
                                 heatMap.transition().duration(1000)
                                     .style("fill", function (d) {
                                         if (d.expected)
                                             return 'rgb(255,255,255)';
                                         else return colorScale(d.corr);
                                         });
                                 expected_show.style("display", "none");
                                 expected_hide.style("display", null);
                             });

        only_expected.on("click", function () {
                                 heatMap.transition().duration(1000)
                                     .style("fill", function (d) {
                                         if (d.expected && -0.5 < d.corr && d.corr < 0.5 )
                                             return colorScale(d.corr);
                                         else return 'rgb(255,255,255)';
                                         });
                                 only_expected.style("display", "none");
                                 expected_hide.style("display", null);
                             });

        expected_hide.on("click", function () {
                                 heatMap.transition().duration(1000)
                                     .style("fill", function (d) { return colorScale(d.corr); });
                                 expected_show.style("display", null);
                                 expected_hide.style("display", "none");
                                 only_expected.style("display", null);
                            });

    };
})(globals);