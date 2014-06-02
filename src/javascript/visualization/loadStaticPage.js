var loadStaticPage = function () {
    // need a drop-down of our surveys
    // file loader for user-defined surveys
    // on click, launch java process in the background
    // if successful, print static analyses
    // if not, print error

    $("#main").hide();
    $("#main").append(
        "<div class=\"dropdown\"><button class=\"btn dropdown-toggle sr-only\" type=\"button\" id=\"dropdownMenu1\" data-toggle=\"dropdown\">Dropdown<span class=\"caret\"></span></button>"
        + "<ul class=\"dropdown-menu\" role=\"menu\" aria-labelledby=\"dropdownMenu1\">"
        + "<li role=\"presentation\"><a role=\"menuitem\" tabindex=\"-1\" href=\"#\">Action</a></li>"
        + "<li role=\"presentation\" class=\"divider\"></li>"
        + "<li role=\"presentation\"><a role=\"menuitem\" tabindex=\"-1\" href=\"#\">Separated link</a></li></ul></div>");
};