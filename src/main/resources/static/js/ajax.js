window.POST = function (option) {
    $.ajax({
        type: "POST",
        url: option.url,
        data: option.data,
        beforeSend: function (request) {
            request.setRequestHeader("content-Type", "application/x-www-form-urlencoded");
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