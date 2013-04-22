function toggleinfo(info){
	if($(info).children('.postinfo-regdate').hasClass('extended')){
		$(info).children('.avatar-cell').removeClass('extended');
		$(info).children('.avatar-cell').children('.avatar').removeClass('extended');
		$(info).children('.postinfo-regdate').removeClass('extended');
		$(info).children('.postinfo-title').removeClass('extended');
	}else{
		$(info).children('.avatar-cell').addClass('extended');
		$(info).children('.avatar-cell').children('.avatar').addClass('extended');
		$(info).children('.postinfo-regdate').addClass('extended');
		$(info).children('.postinfo-title').addClass('extended');
	}
}
function toggleoptions(menu){
	$(menu).parent().parent().children('.postoptions').toggleClass('extended');
}
$('document').ready(function(){
$('.postinfo').click(function(){
toggleinfo($(this));
});
$('.postmenu').click(function(){
toggleoptions($(this));
});
});