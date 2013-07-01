$(document).ready(function() {
    $('div[id^="question"]').addClass('questionDiv').hide();
    $('.questionDiv:first').show();
    $('input').click(function(){
        if($(this).parents('.questionDiv').nextAll('.questionDiv').eq(0).length > 0) {
            $(this).parents('.questionDiv').hide();
            $(this).parents('.questionDiv').nextAll('.questionDiv').eq(0).show();
        }
    });
    $('select').change(function(){
        if($(this).parents('.questionDiv').nextAll('.questionDiv').eq(0).length > 0) {
            $(this).parents('.questionDiv').hide();
            $(this).parents('.questionDiv').nextAll('.questionDiv').eq(0).show();
        }
    });
});
