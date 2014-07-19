var display_raw_scores = (function(globals) {

    return function () {

        var zoomResponse = function (d) {

            var dis = $.html("<table id='respZoom' style='margin-top:"+margin.top+";'></table>");
            for (var i = 0 ; i < d.trueResponses.length ; i++) {
                var q = globals.sm.getQuestionById(d.trueResponses[i].q);
                var os = _.map(d.trueResponses[i].opts, function (oid) { return globals.sm.getOptionById(oid); });
                var row = $.html("<tr><td>" + q.text + "</td></tr>");
                for (var j = 0 ; j < os.length ; j++) {
                    row.append("<td>"+os[i].otext+"</td>");
                }
                dis.append(row);
            }
            $(".legend").hide();
            $(".scatter").hide();
            $("#resp").append(dis);

        };

        var height = 400,
            width = height,
            radius = 5,
            trueResponse = function (obj) {
                return obj.q != "q_-1_-1";
            },
            process_data = function (d) {
                return {
                    score : d.score,
                    valid : d.valid,
                    cutoff : d.response.pval,
                    trueResponses : _.filter(JSON.parse(d.response.responses), trueResponse)
                };
            },
            data = _.map(globals.responses, process_data),
            numQs = _.reduce(globals.sm.survey.topLevelBlocks, function (n, b) { return n + b.getAllBlockQuestions().length; }, 0),
            maxY = Math.ceil(_.max(_.map(data, function (_d) { return _d.score; }))),
            minY = Math.floor(_.min(_.map(data, function (_d) { return _d.score; }))),
            xInterval = width / (numQs + 1),
            yInterval = height / maxY;


        var svg = d3.select("#resp").append("svg")
            .attr("width", width + margin.left + margin.right + 10)
            .attr("height", height + margin.top + margin.bottom)
          .append("g")
            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

        var cValue = function(d) { return d.valid; },
            color = function (d) { return {true : 'green', false : 'red'}[cValue(d)]; };

        // x-axis
        var xValue = function(d) { return d.trueResponses.length; }, // data -> value
            xScale = d3.scale.linear().range([0, width]).domain([0, numQs+1]), // value -> display
            xMap = function(d) { return xScale(xValue(d)); }, // data -> display
            xAxis = d3.svg.axis().scale(xScale).orient("bottom");

        svg.append("g")
              .attr("class", "x axis")
              .attr("transform", "translate(0," + (height + 2) + ")")
              .call(xAxis)
            .append("text")
              .attr("class", "label")
              .attr("x", width)
              .attr("y", -6)
              .style("text-anchor", "end")
              .text("Number of Questions Answered");

        //y-axis

        var yValue = function(d) { return d.score;}, // data -> value
            yScale = d3.scale.linear().range([height, 0]).domain([minY, maxY]), // value -> display
            yMap = function(d) { return yScale(yValue(d)); }, // data -> display
            yAxis = d3.svg.axis().scale(yScale).orient("left");

        svg.append("g")
              .attr("class", "y axis")
              .call(yAxis)
            .append("text")
              .attr("class", "label")
              .attr("transform", "rotate(-90)")
              .attr("y", 6)
              .attr("dy", ".71em")
              .style("text-anchor", "end")
              .text("Score");

        var dataPoints = svg.selectAll("circle")
              .data(data)
            .enter().append("circle")
              .attr("r", radius)
              .attr("cx", xMap)
              .attr("cy", yMap)
              .attr("stroke", "black")
              .attr("stroke-width", 1)
              .style("fill", color)
              .attr("cursor", "pointer")
              .on("click", zoomResponse);

        dataPoints.append("title").text(function (d) { return "(" + xValue(d) + "," + yValue(d) + ")"; });

          // draw legend
        var legend = d3.selectAll("#resp")
            .append("svg")
            .attr("width", 100)
            .attr("height", 100)
            .append("g");

        legend.selectAll(".legend")
              .data(color)
            .enter().append("g")
              .attr("class", "legend")
              .attr("transform", function(d, i) { return "translate(0," + i * 100 + ")"; });


          // draw legend colored rectangles
        legend.append("rect")
              .attr("x", width - 18)
              .attr("width", 18)
              .attr("height", 18)
              .style("fill", color);

          // draw legend text
        legend.append("text")
              .attr("x", width - 24)
              .attr("y", 9)
              .attr("dy", ".35em")
              .style("text-anchor", "end")
              .text(function(d) { return d ? "Valid" : "Invalid";});

    };
})(globals);