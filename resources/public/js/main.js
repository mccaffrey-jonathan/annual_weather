function init() {
    $.get("data", function(data) {
        $(".result").html(JSON.stringify(data));
    });
}

$(document).ready(init);
