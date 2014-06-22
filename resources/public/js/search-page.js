
function onPageLoad() {
    $('#place-search-bar').bind('submit',function(e) {
        var humanPlace = $(this).val();
        var params = {
            'place': humanPlace,
        };
        window.location.href = '/site/chart?' + $.param(params);
    }).keyup(function(e) {
        if(e.keyCode == 13) {
            $(this).trigger('submit');
        }
    });

    $('#place-search-submit').click(function() {
        $('#place-search-bar').trigger("submit");
    });
}

$(document).ready(onPageLoad);
