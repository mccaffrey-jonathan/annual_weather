var monthDictionary = {
    0: "Jan",
    1: "Feb",
    2: "Mar",
    3: "Apr",
    4: "May",
    5: "Jun",
    6: "Jul",
    7: "Aug",
    8: "Sep",
    9: "Oct",
    10: "Nov",
    11: "Dec"
};

function isOdd(n) {
    return (n % 2) == 1;
}

function formatMonths(d) {
    return monthDictionary[d]
}

function formatFahrenheit(d) {
  return d + "\u00B0F";
}

// TODO I really prefer just in front of decimal
var formatNumber = d3.format(".1f");

function ncdcToFahrenheit(ncdc) {
    return ((9*ncdc)/50) + 32;
}

function appendLabel(svg) {
    var cityAndState = "Los Angeles, CA";
    var latAndLong = "43\u00B0N 37\u00B0W";
    var climate = "Temperate Climate";
    var flavorText = "300 Days of Sunshine";
    var url = "wby.com/LosAngelesCA";

    var label = svg.append("g")
        .classed("label", true)
        .attr("transform", "translate(680, 290)");

    var labelBody = label.append("foreignObject")
        .attr("width", 500)
        .attr("height", 500)
        .append("xhtml:body")
        .append("span")
        .style("display", "inline-block")
        .style("text-align", "center");

    // TODO templates
    bodyString = 
        '<div class="labeltop">' +
            cityAndState + '<br/>' +
            latAndLong + 
        '</div>' +
        // '<hr/>' +
        '<div class="labelmid">' +
            climate + '<br/>' + 
            flavorText +
        "</div>" +
        // '<hr/>' +
        '<div class="labelbot">' +
            '<a href="' + url + '">' + url + '</a>' +
        "</div>";

    labelBody.append("div")
        .html(bodyString);

//    labelBody.append("h3")
//        .html(cityAndState);
//
//    labelBody.append("h4")
//        .html(latAndLong);
//
//    labelBody.append("hr");
//
//    labelBody.append("h6")
//        .html(climate);
//
//    labelBody.append("h6")
//        .html(flavorText);
//
//    labelBody.append("hr");
//
//    labelBody.append("a")
//        .html(url);
}

function init() {

    $.get("data", function(data) {

        // Convert strings to numbers.
        data.forEach(function(d) {
            d.key = +d.key;
            d.value.MNTM = ncdcToFahrenheit(d3.mean(d.value.MNTM));
            d.value.MMXT = ncdcToFahrenheit(d3.mean(d.value.MMXT));
            d.value.MMNT = ncdcToFahrenheit(d3.mean(d.value.MMNT));
            d.value.EMXT = ncdcToFahrenheit(d3.mean(d.value.EMXT));
            d.value.EMNT = ncdcToFahrenheit(d3.mean(d.value.EMNT));
        });

        // TODO start graph setup async from data fetching
        // $(".result").html(JSON.stringify(data));

        var yAxisTextOffset = 45,
            margin = {top: 20, right: 40, bottom: 30, left: yAxisTextOffset},
            width = 960 - margin.left - margin.right,
            height = 500 - margin.top - margin.bottom,
            barSpace = Math.floor(width / data.length) - 1,
            barWidth = (3*barSpace)/4;

        var x = d3.scale.linear()
            .domain([0, 12])
            .range([0, width]);

        // Inverted range, Bigger is up
        var y = d3.scale.linear()
            .domain([-20, 120])
            .range([height, 0]);

        // An SVG element with a bottom-right origin.
        var svg = d3.select(".result").append("svg")
            .attr("width", width + margin.left + margin.right)
            .attr("height", height + margin.top + margin.bottom)
          .append("g")
            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

        // tickSize width drives the ticks across the entire chart
        var yAxis = d3.svg.axis()
            .scale(y)
            .ticks(20)
            .tickSize(width)
            .tickFormat(formatFahrenheit)
            .orient("right");

        var gy = svg.append("g")
            .attr("class", "y axis")
            .call(yAxis);

        gy.selectAll("g")
            .filter(function(d) { return d; })
            .classed("minor", true);

        gy.selectAll("g")
            .filter(function(d) { return d === 0; })
            .classed("major", true);

        gy.selectAll("text")
            .attr("x", -yAxisTextOffset)
            .attr("dy", 4);

        var xAxis = d3.svg.axis()
            .scale(x)
            .tickSize(-height)
            .tickSubdivide(true)
            .tickFormat(formatMonths);

        // Add the x-axis.
        var gx = svg.append("g")
            .attr("class", "x axis")
            .attr("transform", "translate(0," + height + ")")
            .call(xAxis);
        
        gx.filter(isOdd).classed("odd", true);

        gx.selectAll("text")
            .attr("x", barWidth/2)
            .style("text-anchor", "middle");

        gx.selectAll("g").filter(function(d) { return d; })
            .classed("hidden", true);

        var bucket = svg.selectAll(".bucket")
            .data(data);

        var bucketEnter =
            bucket.enter()
            .append("g")
            .classed("bucket", true)
            .attr("transform", function(d) {
                return "translate(" + x(d.key) + ",0)";
            })
            .append("g")
            // Center on middle of bar..
            .attr("transform", function(d) {
                return "translate(" + barSpace/2 + ",0)";
            });

        bucketEnter.append("rect").attr("class", "box");
        bucket.select(".box")
          .attr("width", barWidth)
          .attr("height", function (d) {
                  return y(d.value.MMNT) - y(d.value.MMXT);
          })
          .attr("x", -barWidth/2)
          .attr("y", function(d) {
              return y(d.value.MMXT);
          });

        // TODO these pinch over the rect stroke a bit...
        bucketEnter.append("line").attr("class", "middle");
        bucket.select(".middle")
          .attr("x1", -barWidth/2)
          .attr("x2", barWidth/2)
          .attr("y1", function(d) {
              return y(d.value.MNTM);
          })
          .attr("y2", function(d) {
              return y(d.value.MNTM);
          });

        bucketEnter.append("line").attr("class", "whisker-line top");
        bucketEnter.append("line").attr("class", "whisker-line bot");
        bucket.selectAll(".whisker-line")
          .attr("x1", 0)
          .attr("x2", 0)
          .attr("stroke-dasharray", "3, 2");

        bucket.filter(function (d) {return isOdd(d.key); })
            .classed("odd", true);

        bucket.selectAll(".whisker-line.top")
          .attr("y1", function (d) {
              return y(d.value.MMXT);
          })
          .attr("y2", function(d) {
              return y(d.value.EMXT);
          });

        bucket.selectAll(".whisker-line.bot")
          .attr("y1", function (d) {
              return y(d.value.MMNT);
          })
          .attr("y2", function(d) {
              return y(d.value.EMNT);
          });

        bucketEnter.append("line").attr("class", "whisker-end top");
        bucketEnter.append("line").attr("class", "whisker-end bot");
        bucket.selectAll(".whisker-end")
          .attr("x1", -barSpace/4)
          .attr("x2", barSpace/4);

        bucket.selectAll(".whisker-end.top")
          .attr("y1", function (d) {
              return y(d.value.EMXT);
          })
          .attr("y2", function(d) {
              return y(d.value.EMXT);
          });

        bucket.selectAll(".whisker-end.bot")
          .attr("y1", function (d) {
              return y(d.value.EMNT);
          })
          .attr("y2", function(d) {
              return y(d.value.EMNT);
          });

        appendLabel(svg);

    });
}

$(document).ready(init);
