window.POST = function (url, data, success) {
    $.ajax({
        type: "POST",
        url,
        data,
        beforeSend: function (request) {
            request.setRequestHeader("content-Type", "application/x-www-form-urlencoded");
        },
        success: function (result) {
            success(result);
        }
    });
}