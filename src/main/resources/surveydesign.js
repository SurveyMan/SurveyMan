/**
 * Created by etosch on 1/25/15.
 */

var drag_toable = function (event) { event.preventDefault(); }

var drag_block = function(event) {

    event.dataTransfer.setData("text", ev.target.id);

}

var drop_block = function(event, parent) {

    //var new_block = SurveyMan.survey.Block.new_block(parent);
    event.preventDefault();
    var data = ev.dataTransfer.getData("text");
    ev.target.appendChild(document.getElementById(data));
}

function () {
//http://stackoverflow.com/questions/1108480/svg-draggable-using-jquery-and-jquery-svg
$('block')
    .draggable()
  .bind('mousedown', function(event, ui){
    // bring target to front
    $(event.target.parentElement).append( event.target );
  })
  .bind('drag', function(event, ui){
    // update coordinates manually, since top/left style props don't work on SVG
    event.target.setAttribute('x', ui.position.left);
    event.target.setAttribute('y', ui.position.top);
        event.target.setAttribute('cx', ui.position.left);
        event.target.setAttribute('cy', ui.position.top);

  })

  $('#content')
  .bind('drop', function(event, ui, parent) {
    drop_block(event, parent);
    event.target.setAttribute('x', ui.position.left);
    event.target.setAttribute('y', ui.position.top);
        event.target.setAttribute('cx', ui.position.left);
        event.target.setAttribute('cy', ui.position.top);
  });
  }();