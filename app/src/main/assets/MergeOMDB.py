# coding:utf-8
# 合并指定目录下的omdb（sqlite）数据

import os
import sys
import json
import sqlite3


# 定义遍历目录的函数
def traverse_dir(path):
    fileList = list()
    for root, dirs, files in os.walk(path):
        for file in files:
            if str(file).endswith(".omdb"):
                # 文件的完整路径
                file_path = os.path.join(root, file)
                # 处理文件，例如读取文件内容等
                print(file_path)
                fileList.append(file_path)
    return fileList


# 打开配置文件，读取用户配置的
def openConfigJson(path):
    # 读取json配置，获取要抽取的表名
    with open(path, "r") as f:
        configMap = json.load(f)
        return configMap


# 按照tableList中指定的表名合并多个源数据库到指定目标数据库中
def mergeSqliteData(originSqliteList, destSqlite, tableList):
    destConn = sqlite3.connect(destSqlite)
    destCursor = destConn.cursor()

    for originSqlite in originSqliteList:
        originConn = sqlite3.connect(originSqlite)
        originCursor = originConn.cursor()
        # 从源数据库中遍历取出表list中的数据
        for table in tableList:
            # 检查目标数据库中是否存在指定的表
            containsTable = destCursor.execute(
                "SELECT sql FROM sqlite_master WHERE type='table' AND name='%s'" % (table)).fetchall()
            if not containsTable or len(containsTable) <= 0:
                # 复制表结构
                originCursor.execute("SELECT sql FROM sqlite_master WHERE type='table' AND name='%s'" % (table))
                createTableSql = originCursor.fetchone()[0]
                destCursor.execute(createTableSql)
                destConn.commit()

            originCursor.execute("Select * From " + table)
            # 获取到源数据库中该表的所有数据
            originData = originCursor.fetchall()
            # 获取一行数据中包含多少列，以此动态设置sql语句中的？个数
            if originData and len(originData)>0:
                num_cols = len(originData[0])
                placeholders = ",".join(["?"] * num_cols)
                for row in originData:
                    destCursor.execute("INSERT INTO "+table+" VALUES ({})".format(placeholders), row)

        print("{}数据已导入！".format(originSqlite))
        originCursor.close()
        originConn.close()
        destConn.commit()
    destCursor.close()
    destConn.close()


if __name__ == '__main__':
    params = sys.argv[1:]  # 截取参数
    if params:
        if not params[0]:
            print("请输入要合并的omdb数据的文件夹")
            raise AttributeError("请输入要合并的omdb数据的文件夹")
        # 获取导出文件的表配置
        jsonPath = params[0] + "/config.json"
        if not os.path.exists(jsonPath):
            raise AttributeError("指定目录下缺少config.json配置文件")
        omdbDir = params[0]
        originSqliteList = traverse_dir(omdbDir)  # 获取到所有的omdb数据库的路径

        tableNameList = list()
        configMap = openConfigJson(jsonPath)
        if configMap["tables"] and len(configMap["tables"]) > 0:
            for tableName in set(configMap["tables"]):
                tableNameList.append(tableName)
            print(tableNameList)
        else:
            raise AttributeError("config.json文件中没有配置抽取数据的表名")

        # 开始分别连接Sqlite数据库，按照指定表名合并数据
        mergeSqliteData(originSqliteList, params[0]+"/output.sqlite", tableNameList)
    else:
        raise AttributeError("缺少参数：请输入要合并的omdb数据的文件夹")
