window.POST = function (option) {
    $.ajax({
        type: "POST",
        url: option.url,
        data: JSON.stringify(option.data),
        // contentType: "application/json; charset=UTF-8",
        contentType:"application/x-www-form-urlencoded; charset=UTF-8",
        beforeSend: function (request) {
            // request.setRequestHeader("application/x-www-form-urlencoded; charset=UTF-8");
        },
        success: function (result) {
            if (result && result.code === 200) {
                option.success && option.success(result);
                option.finally && option.finally(result);
            } else {
                option.fail && option.fail(result);
                option.finally && option.finally(result);
            }
        }
    });
}