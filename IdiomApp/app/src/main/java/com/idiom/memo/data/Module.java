package com.idiom.memo.data;

public enum Module {
    CURRENT_AFFAIRS("current_affairs", "时政", "知识点", "内容", true,
            "注：建议通过豆包等大模型总结生成，最好不要自己手动输入，建议上传md文档。"),
    COMMON_SENSE("common_sense", "常识", "知识点", "内容", true,
            "注：建议通过豆包等大模型总结生成，最好不要自己手动输入，建议上传md文档。"),
    VERBAL("verbal", "言语理解", "知识点", "内容", true,
            "注：建议通过豆包等大模型总结生成，最好不要自己手动输入，建议上传md文档。"),
    QUANTITY("quantity", "数量关系", "知识点", "内容", true,
            "注：建议通过豆包等大模型总结生成，最好不要自己手动输入，建议上传md文档。"),
    JUDGMENT("judgment", "判断推理", "知识点", "内容", true,
            "注：建议通过豆包等大模型总结生成，最好不要自己手动输入，建议上传md文档。"),
    DATA_ANALYSIS("data_analysis", "资料分析", "知识点", "内容", true,
            "注：建议通过豆包等大模型总结生成，最好不要自己手动输入，建议上传md文档。");

    public final String id;
    public final String displayName;
    public final String termLabel;
    public final String contentLabel;
    public final boolean enabled;
    public final String uploadHint;

    Module(String id, String displayName, String termLabel, String contentLabel,
           boolean enabled, String uploadHint) {
        this.id = id;
        this.displayName = displayName;
        this.termLabel = termLabel;
        this.contentLabel = contentLabel;
        this.enabled = enabled;
        this.uploadHint = uploadHint;
    }

    public String allTitle() {
        return this == VERBAL ? "我的错题本" : "我的知识库";
    }

    public String allItemsLabel() {
        return this == VERBAL ? "全部错题" : "全部知识点";
    }

    public String listTitle() {
        return this == VERBAL ? "错题列表" : "知识列表";
    }

    public String statsTitle() {
        return this == VERBAL ? "错题状态" : "学习状态";
    }

    public String cardModeTitle() {
        return this == VERBAL ? "刷内容卡片（错题）" : "刷内容卡片";
    }

    public String quizModeTitle() {
        return this == VERBAL ? "做选择题练习（错题）" : "做选择题练习";
    }

    public String uploadEntryLabel() {
        return this == VERBAL ? "上传 MD 导入知识点（错题模块）"
                : "上传 MD 导入" + termLabel;
    }

    public String pickFileTitle() {
        return this == VERBAL ? "点击选择 .md 文件（只针对错题）" : "点击选择 .md 文件";
    }

    public String templateFileName() {
        return this == VERBAL ? "下载言语理解导入模板（错题）.md"
                : "下载" + displayName + "导入模板.md";
    }

    public String searchHint() {
        return this == VERBAL ? "搜索错题" : "搜索知识点";
    }

    public String addTitle() {
        return "添加" + termLabel;
    }

    public static Module fromId(String id) {
        for (Module m : values()) {
            if (m.id.equals(id)) return m;
        }
        return VERBAL;
    }
}
