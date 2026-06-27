#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
yadapm(Oracle) -> newpm(MySQL) 历史数据迁移 ETL (spec 012, A 反范式化文本快照)
- Oracle 取数: 因 oracledb thin 不支持 11.2, 改用容器内 sqlplus + CHR(1) 分隔导出(NLS_LANG=AL32UTF8)
- MySQL 写入: pymysql
- 幂等: 各表迁移前删除自己迁过的(create_by='yadapm-migrate' 或全表-仅纯归档表)
- 目标库由参数指定(彩排灌 ry_vue_prod, 不碰真生产)
用法: /tmp/etl-venv/bin/python pm-sql/migration_012/etl.py [ry_vue_prod]
"""
import subprocess, sys, datetime

MARK = 'yadapm-migrate'          # 迁移标记(create_by), 用于幂等
ORA = ['docker','exec','-i','-e','NLS_LANG=AMERICAN_AMERICA.AL32UTF8','yadapm-oracle','bash','-c','sqlplus -S YADAPM/yadapm@localhost/XE']
TARGET_DB = sys.argv[1] if len(sys.argv) > 1 else 'ry_vue_prod'

import pymysql
mysql = pymysql.connect(host='127.0.0.1', port=3306, user='root', password='password',
                        database=TARGET_DB, charset='utf8mb4', autocommit=False)

DELIM = '\x01'

def ora_extract(table, cols, where=None):
    """从 Oracle 取表数据, 返回 list[dict]. 文本内换行替换为空格, CHR(1) 分隔."""
    exprs = []
    for c in cols:
        exprs.append(f"TRIM(REPLACE(REPLACE(NVL(TO_CHAR({c}),''),CHR(10),' '),CHR(13),' '))")
    # 每列一行(列间用 ||CHR(1)|| 连接), 避免 sqlplus 单行 >2499 限制
    sel = " || CHR(1) ||\n".join(exprs)
    sql = f"SET PAGESIZE 0 HEADING OFF FEEDBACK OFF LINESIZE 32767 LONG 32767 TRIMSPOOL ON SQLBLANKLINES ON\n"
    sql += f"SELECT\n{sel}\nFROM {table}"
    if where:
        sql += f"\nWHERE {where}"
    sql += ";\nEXIT;\n"
    r = subprocess.run(ORA, input=sql, capture_output=True, text=True)
    rows = []
    for line in r.stdout.split('\n'):
        if DELIM not in line:
            continue
        parts = line.split(DELIM)
        if len(parts) != len(cols):
            # 列数不符, 跳过(可能是包裹/异常行)
            continue
        d = {c: (p if p != '' else None) for c, p in zip(cols, parts)}
        rows.append(d)
    return rows

def parse_date(s):
    """yyyymmdd / yyyy-mm-dd / yyyymmddhhmmss -> 'YYYY-MM-DD'; 非法->None"""
    if not s:
        return None
    s = s.strip()
    digits = s.replace('-', '').replace('/', '').replace(':', '').replace(' ', '')
    if len(digits) >= 8 and digits[:8].isdigit():
        y, m, d = digits[0:4], digits[4:6], digits[6:8]
        try:
            datetime.date(int(y), int(m), int(d))
            return f"{y}-{m}-{d}"
        except ValueError:
            return None
    return None

def parse_dt(s):
    """yyyymmddhhmmss -> 'YYYY-MM-DD HH:MM:SS'; 退化到日期; 非法->None"""
    if not s:
        return None
    digits = ''.join(ch for ch in s if ch.isdigit())
    if len(digits) >= 14:
        y, mo, d, h, mi, se = digits[0:4], digits[4:6], digits[6:8], digits[8:10], digits[10:12], digits[12:14]
        try:
            datetime.datetime(int(y), int(mo), int(d), int(h), int(mi), int(se))
            return f"{y}-{mo}-{d} {h}:{mi}:{se}"
        except ValueError:
            pass
    dd = parse_date(s)
    return f"{dd} 00:00:00" if dd else None

# ---------- 载入解码表 ----------
def load_map(table, kcol, vcol):
    m = {}
    for r in ora_extract(table, [kcol, vcol]):
        m[r[kcol]] = r[vcol]
    return m

print(f"[init] 目标库={TARGET_DB}, 载入解码表...", flush=True)
PRODUCT = load_map('T_C_PRODUCT', 'ID', 'PRODUCT')            # 产品id->名
YEAR = load_map('T_C_YEAR', 'ID', 'YEAR')                     # 年份id->年
BATCH_NO_BY_ID = load_map('T_C_BATCH_STATUS', 'ID', 'BATCH_NO')  # 老批次id->批次号
PRO_DATE_BY_ID = load_map('T_C_BATCH_STATUS', 'ID', 'PRO_DATE')  # 老批次id->投产日期(版本列表"版本投产日期"列显示用)
CENTER_BY_TASKID = {}                                            # 老任务内部id -> 软件中心任务号(CENTER_TASK_NO)
for _r in ora_extract('T_B_TASK', ['TASK_ID', 'CENTER_TASK_NO']):
    _k = str(_r['TASK_ID']).strip().split('.')[0]
    if _k:
        CENTER_BY_TASKID[_k] = _r['CENTER_TASK_NO']
SCHED_MAP = {   # 老排期状态中文 -> 新 sys_pqzt 字典值(已作废无对应,保留原文,前端兜底显示)
    '已排期待投产': '1', '已投产': '2',
    '未排期已征求意见': '3', '未排期-未征求排期意见': '4', '未排期未征求意见': '4',
}
def map_sched(s):
    if not s:
        return s
    return SCHED_MAP.get(str(s).strip(), s)   # 映射不到(如"已作废")保留原文
def decode_task_nos(s):
    """老 TASK_NO 是逗号分隔的任务内部id串(如 '763,762') -> 解码成软件中心任务号串(M-201901-89144,...)"""
    if not s:
        return s
    out = []
    for p in str(s).replace('，', ',').split(','):
        p = p.strip().split('.')[0]
        if not p:
            continue
        out.append(CENTER_BY_TASKID.get(p, p))   # 命中解码,未命中保留原值
    return ','.join(out) if out else s
# 新系统 overlap 映射: task_code->新task_id, batch_no->新batch_id
cur = mysql.cursor()
cur.execute("SELECT TRIM(task_code), MIN(task_id) FROM pm_task WHERE task_code IS NOT NULL GROUP BY TRIM(task_code)")
NEW_TASK = {k: v for k, v in cur.fetchall()}
cur.execute("SELECT batch_no, MIN(batch_id) FROM pm_production_batch WHERE batch_no IS NOT NULL GROUP BY batch_no")
NEW_BATCH = {k: v for k, v in cur.fetchall()}
USER_NAME_BY_ID = load_map('T_N_SHIRO_USER', 'USER_ID', 'USER_NAME')  # 老用户id->姓名
print(f"[init] PRODUCT={len(PRODUCT)} YEAR={len(YEAR)} BATCH_NO_BY_ID={len(BATCH_NO_BY_ID)} 用户={len(USER_NAME_BY_ID)} 新任务={len(NEW_TASK)} 新批次={len(NEW_BATCH)}", flush=True)

def year_of(year_id):
    return YEAR.get(year_id)  # '2026' / '待定' etc.

def uname(uid):
    return USER_NAME_BY_ID.get(uid)  # 老录入人id->姓名

report = []  # (功能, 源表, 源数, 迁入, FK挂上, 快照, 失败)

def insert_rows(table, col_list, rows, label, uniq_idx=None, uniq_maxlen=160):
    """批量插入, 一条不丢: 撞唯一键(1062)时给 uniq_idx 列加后缀 _R2/_R3... 重试.
    返回(成功, 失败, 加后缀条数). uniq_idx=None 则不重试(无唯一键表)."""
    placeholders = ','.join(['%s'] * len(col_list))
    sql = f"INSERT INTO {table} ({','.join(col_list)}) VALUES ({placeholders})"
    ok = fail = suffixed = 0
    errs = {}
    for vals in rows:
        vals = list(vals)
        base = vals[uniq_idx] if uniq_idx is not None else None
        attempt = 0
        while True:
            try:
                cur.execute(sql, vals)
                ok += 1
                if attempt > 0:
                    suffixed += 1
                break
            except pymysql.err.IntegrityError as e:
                if e.args and e.args[0] == 1062 and uniq_idx is not None and attempt < 50 and base:
                    attempt += 1
                    suf = f"_R{attempt+1}"
                    vals[uniq_idx] = base[:max(1, uniq_maxlen - len(suf))] + suf
                    continue
                fail += 1
                errs[str(e)[:60]] = errs.get(str(e)[:60], 0) + 1
                break
            except Exception as e:
                fail += 1
                errs[str(e)[:60]] = errs.get(str(e)[:60], 0) + 1
                break
    mysql.commit()
    if errs:
        print(f"  [{label}] 失败样例: {dict(list(errs.items())[:3])}", flush=True)
    if suffixed:
        print(f"  [{label}] 撞键加后缀保留: {suffixed} 条", flush=True)
    return ok, fail, suffixed

# ========== ③ T_B_OLD_VERSION_OUT -> pm_old_version_out (纯文本快照) ==========
def etl_old_version_out():
    src = ora_extract('T_B_OLD_VERSION_OUT', [
        'SYS_NAME','SUB_VERSION_CODE','BASE_VERSION_CODE','OUT_LIB_VERSION','VERSION_TYPE','VERSION_CODE',
        'COMM_NAME','VERSION_P_DATE','VERSION_DESCR','REMARKS','TASK_NO','TASK_NAME','PRO_YEAR','PRO_BATCH_NO',
        'IS_INVOLVED','DB_UPDATE','USB_UPDATE','SEQUENCE_NO'])
    cur.execute("DELETE FROM pm_old_version_out")
    cols = ['sys_name','product','base_version_code','out_lib_version','version_type','version_code',
            'comm_name','version_p_date','version_descr','remarks','task_no','task_name','pro_year','pro_batch_no',
            'is_involved','db_update','usb_update','sequence_no']
    rows = []
    for r in src:
        rows.append([r['SYS_NAME'], r['SUB_VERSION_CODE'], r['BASE_VERSION_CODE'], r['OUT_LIB_VERSION'],
                     r['VERSION_TYPE'], r['VERSION_CODE'], r['COMM_NAME'], r['VERSION_P_DATE'], r['VERSION_DESCR'],
                     r['REMARKS'], r['TASK_NO'], r['TASK_NAME'], r['PRO_YEAR'], r['PRO_BATCH_NO'],
                     r['IS_INVOLVED'], r['DB_UPDATE'], r['USB_UPDATE'], r['SEQUENCE_NO']])
    ok, fail, sfx = insert_rows('pm_old_version_out', cols, rows, '③旧数据查询')
    report.append(('③旧数据查询','T_B_OLD_VERSION_OUT', len(src), ok, '-', ok, fail, sfx))

# ========== ①② T_B_VERSION_OUT -> pm_version_out ==========
def etl_version_out():
    src = ora_extract('T_B_VERSION_OUT', [
        'SYS_NAME','BASE_VERSION_CODE','OUT_LIB_VERSION','OUT_VERSION','VERSION_TYPE','VERSION_CODE','VERSION_DESCR',
        'COMM_NAME','VERSION_P_DATE','TASK_NO','TASK_NAME','PRO_YEAR','PRO_BATCH_NO','DB_UPDATE','USB_UPDATE',
        'SEQUENCE_NO','IS_INVOLVED','SUB_VERSION_CODE','MANUAL_INPUT','REMARKS','CREATION_DATE',
        'LAST_MODIFICATION_DATE','PACKAGE_MODE','VERSION_STATUS','BATCH_ID'])
    cur.execute(f"DELETE FROM pm_version_out WHERE create_by='{MARK}'")
    cols = ['production_year','batch_id','pro_batch_no','sub_version_code','product','manual_task_no','manual_task_name',
            'version_type','sys_name','base_version_code','out_lib_version','version_code','out_version','comm_name',
            'comm_name_display','version_p_date','is_involved','db_update','usb_update','package_mode','version_status',
            'version_brief','version_descr','remarks','manual_input','del_flag','create_by','create_time','update_time']
    rows = []
    fk = snap = 0
    for r in src:
        mi = r['MANUAL_INPUT'] or '0'
        prod = PRODUCT.get(r['SUB_VERSION_CODE'])               # 解码产品名
        # 投产批次号: 老列表显示的是 BATCH_ID JOIN T_C_BATCH_STATUS.BATCH_NO(1612全有),非原表空列 PRO_BATCH_NO
        old_batch_no = BATCH_NO_BY_ID.get(r['BATCH_ID']) or r['PRO_BATCH_NO']
        new_bid = NEW_BATCH.get(old_batch_no)                   # 批次以新系统为准(历史批次多数不在新表,挂不上走快照)
        if new_bid: fk += 1
        else: snap += 1
        # 版本投产日期: 老列表显示批次表 PRO_DATE(按BATCH_ID),非原表 VERSION_P_DATE
        version_p_date = parse_date(PRO_DATE_BY_ID.get(r['BATCH_ID']) or r['VERSION_P_DATE'])
        # 坑9: TASK_NAME 批次=版本简介 / 非批次=任务名称
        if mi == '1':
            mtask_no, mtask_name, vbrief = r['TASK_NO'], r['TASK_NAME'], None   # 非批次:任务号手填文本,保留原值
        else:
            mtask_no, mtask_name, vbrief = decode_task_nos(r['TASK_NO']), None, r['TASK_NAME']  # 批次:TASK_NO是内部id串,解码成软件中心任务号
        rows.append([year_of(r['PRO_YEAR']), new_bid, old_batch_no, prod, prod, mtask_no, mtask_name,
                     r['VERSION_TYPE'], r['SYS_NAME'], r['BASE_VERSION_CODE'], r['OUT_LIB_VERSION'], r['VERSION_CODE'],
                     r['OUT_VERSION'], r['COMM_NAME'], uname(r['COMM_NAME']), version_p_date, r['IS_INVOLVED'], r['DB_UPDATE'],
                     r['USB_UPDATE'], r['PACKAGE_MODE'], r['VERSION_STATUS'], vbrief, r['VERSION_DESCR'], r['REMARKS'],
                     mi, '0', MARK, parse_dt(r['CREATION_DATE']), parse_dt(r['LAST_MODIFICATION_DATE'])])
    # out_lib_version 在 cols 中索引=10, varchar(64); 撞唯一键(sys_name+version_type+out_lib_version)加后缀
    ok, fail, sfx = insert_rows('pm_version_out', cols, rows, '①②版本管理', uniq_idx=10, uniq_maxlen=64)
    report.append(('①②版本管理','T_B_VERSION_OUT', len(src), ok, fk, snap, fail, sfx))

# ========== ④ T_B_PROLIST_AND_DEFECT -> pm_prolist_defect (任务快照来自老T_B_TASK) ==========
def etl_prolist():
    # 关联老任务取快照 + 老批次号(经 Oracle 视图 V_PROLIST_MIG, 避免 sqlplus 单行超长)
    src = ora_extract(
        'V_PROLIST_MIG',
        ['PROBLEM_NO','SUBMIT_DATE','SETTLE_DATE','DEFECT_DESC','VERIFY_DATE','WHETHER_DEFECT','WHETHER_OVERTIME',
         'WHETHER_PRO_RECURRENCE','WHETHER_ATT_REQUIRED','SOLUTINO_TIME_OVER_ONE_DAY','CREATION_DATE',
         'LAST_MODIFICATION_DATE','YEAR_ID','BATCH_NO','CURRENT_STATUS_ID','REMARKS','PROBLEM_LEVEL_ID',
         'UPDATE_VERSION','BATCH_ID','TKNO','TKNAME','TKPROD','TSUBB','TVER','TPRO','TSCH',
         'SUBTASK_TEAM','CREATOR','MODIFIER','TC_PRO_DATE'])
    cur.execute(f"DELETE FROM pm_prolist_defect WHERE create_by='{MARK}'")
    cols = ['task_id','task_no','task_name','product','internal_closure_date','functional_test_date',
            'production_version_date','schedule_status','problem_no','problem_level','current_status','submit_date',
            'settle_date','verify_date','whether_defect','whether_overtime','whether_pro_recurrence',
            'whether_att_required','whether_update_version','solution_time_over_one_day','defect_desc',
            'production_year','batch_id','pro_batch_no','remarks','del_flag','create_by','create_time','update_time',
            'subtask_team','plan_production_date','creator_name','modifier_name']
    rows = []
    fk = snap = 0
    for r in src:
        tkno = r['TKNO']
        new_tid = NEW_TASK.get(tkno) if tkno else None          # 任务以新系统为准
        old_batch_no = r['BATCH_NO'] or BATCH_NO_BY_ID.get(r['BATCH_ID'])
        new_bid = NEW_BATCH.get(old_batch_no)
        if new_tid or new_bid: fk += 1
        else: snap += 1
        rows.append([new_tid, tkno, r['TKNAME'], r['TKPROD'], parse_date(r['TSUBB']), parse_date(r['TVER']),
                     parse_date(r['TPRO']), map_sched(r['TSCH']), (r['PROBLEM_NO'] or '')[:160], r['PROBLEM_LEVEL_ID'],
                     r['CURRENT_STATUS_ID'], parse_date(r['SUBMIT_DATE']), parse_date(r['SETTLE_DATE']),
                     parse_date(r['VERIFY_DATE']), r['WHETHER_DEFECT'], r['WHETHER_OVERTIME'],
                     r['WHETHER_PRO_RECURRENCE'], r['WHETHER_ATT_REQUIRED'], r['UPDATE_VERSION'],
                     r['SOLUTINO_TIME_OVER_ONE_DAY'], r['DEFECT_DESC'], year_of(r['YEAR_ID']), new_bid, old_batch_no,
                     r['REMARKS'], '0', MARK, parse_dt(r['CREATION_DATE']), parse_dt(r['LAST_MODIFICATION_DATE']),
                     r['SUBTASK_TEAM'], parse_date(r['TC_PRO_DATE']), uname(r['CREATOR']), uname(r['MODIFIER'])])
    # problem_no 在 cols 中索引=8, varchar(160); 撞唯一键加后缀(含全/半角括号 collation 撞键)
    ok, fail, sfx = insert_rows('pm_prolist_defect', cols, rows, '④批次问题单', uniq_idx=8, uniq_maxlen=160)
    report.append(('④批次问题单','T_B_PROLIST_AND_DEFECT', len(src), ok, fk, snap, fail, sfx))

# ========== ⑤ T_B_NOBATCH_PROLIST_AND_DEFECT -> pm_nobatch_prolist_defect ==========
def etl_nobatch():
    src = ora_extract('V_NOBATCH_MIG', [
        'PROBLEM_NO','SUBMIT_DATE','SETTLE_DATE','DEFECT_DESC','VERIFY_DATE','WHETHER_DEFECT','WHETHER_OVERTIME',
        'WHETHER_PRO_RECURRENCE','WHETHER_ATT_REQUIRED','SOLUTINO_TIME_OVER_ONE_DAY','CREATION_DATE',
        'LAST_MODIFICATION_DATE','YEAR_ID','CURRENT_STATUS_ID','REMARKS','PROBLEM_LEVEL_ID','UPDATE_VERSION',
        'BATCH_ID','TASK_NO','TASK_NAME','PRODUCT','TEST_SUB_B_DATE','TEST_VERSION_DATE','PRO_VERSION_DATE',
        'SCHEDULING_STATUS','SUBTASK_TEAM','CREATOR','MODIFIER','TC_PRO_DATE'])
    cur.execute(f"DELETE FROM pm_nobatch_prolist_defect WHERE create_by='{MARK}'")
    cols = ['task_no','task_name','product','internal_closure_date','functional_test_date','production_version_date',
            'schedule_status','problem_no','problem_level','current_status','submit_date','settle_date','verify_date',
            'whether_defect','whether_overtime','whether_pro_recurrence','whether_att_required','whether_update_version',
            'solution_time_over_one_day','defect_desc','production_year','batch_id','pro_batch_no','remarks','del_flag',
            'create_by','create_time','update_time','subtask_team','plan_production_date','creator_name','modifier_name']
    rows = []
    fk = snap = 0
    for r in src:
        old_batch_no = BATCH_NO_BY_ID.get(r['BATCH_ID'])
        new_bid = NEW_BATCH.get(old_batch_no)
        if new_bid: fk += 1
        else: snap += 1
        prod = r['PRODUCT']
        prod = PRODUCT.get(prod, prod)  # 老非批次PRODUCT可能是id或名, 能解码则解码
        rows.append([r['TASK_NO'], r['TASK_NAME'], prod, parse_date(r['TEST_SUB_B_DATE']),
                     parse_date(r['TEST_VERSION_DATE']), parse_date(r['PRO_VERSION_DATE']), r['SCHEDULING_STATUS'],
                     (r['PROBLEM_NO'] or '')[:160], r['PROBLEM_LEVEL_ID'], r['CURRENT_STATUS_ID'],
                     parse_date(r['SUBMIT_DATE']), parse_date(r['SETTLE_DATE']), parse_date(r['VERIFY_DATE']),
                     r['WHETHER_DEFECT'], r['WHETHER_OVERTIME'], r['WHETHER_PRO_RECURRENCE'], r['WHETHER_ATT_REQUIRED'],
                     r['UPDATE_VERSION'], r['SOLUTINO_TIME_OVER_ONE_DAY'], r['DEFECT_DESC'], year_of(r['YEAR_ID']),
                     new_bid, old_batch_no, r['REMARKS'], '0', MARK, parse_dt(r['CREATION_DATE']),
                     parse_dt(r['LAST_MODIFICATION_DATE']), r['SUBTASK_TEAM'], parse_date(r['TC_PRO_DATE']),
                     uname(r['CREATOR']), uname(r['MODIFIER'])])
    # problem_no 在 cols 中索引=7, varchar(160); 撞唯一键加后缀
    ok, fail, sfx = insert_rows('pm_nobatch_prolist_defect', cols, rows, '⑤非批次问题单', uniq_idx=7, uniq_maxlen=160)
    report.append(('⑤非批次问题单','T_B_NOBATCH_PROLIST_AND_DEFECT', len(src), ok, fk, snap, fail, sfx))

# ========== ④附件 T_B_PROLIST_FILE -> pm_attachment(prolist) 仅元数据 ==========
def etl_attachment():
    # 需把老 PROBLEM_ID 映射到迁移后新 problem_id: 用 problem_no 关联
    src = ora_extract('V_PROLIST_FILE_MIG',
        ['FILE_TYPE','FILE_URL','UPLOADER','UPLOAD_DATE','FILE_DES','PROBLEM_NO'])
    cur.execute(f"DELETE FROM pm_attachment WHERE create_by='{MARK}' AND business_type='prolist'")
    # 新问题单 problem_no -> new problem_id
    cur.execute(f"SELECT problem_no, problem_id FROM pm_prolist_defect WHERE create_by='{MARK}'")
    pno2id = {k: v for k, v in cur.fetchall()}
    cols = ['business_type','business_id','document_type','file_name','file_path','file_size','file_type',
            'file_description','del_flag','create_by','create_time','remark']
    rows = []
    matched = 0
    for r in src:
        bid = pno2id.get((r['PROBLEM_NO'] or '')[:160])
        if bid:
            matched += 1
        fname = (r['FILE_URL'] or '').replace('\\', '/').split('/')[-1] if r['FILE_URL'] else None
        ext = ('.' + fname.rsplit('.', 1)[-1]) if (fname and '.' in fname) else None
        rows.append(['prolist', bid, '1', fname, r['FILE_URL'], None, ext, r['FILE_DES'], '0', MARK,
                     parse_dt(r['UPLOAD_DATE']), '迁移自yadapm-物理文件缺失'])
    # business_id 为空的也插(标记孤儿), 但 business_id NOT NULL? 允许NULL则插, 否则跳过
    rows2 = [v for v in rows if v[1] is not None]
    ok, fail, sfx = insert_rows('pm_attachment', cols, rows2, '④附件')
    report.append(('④附件','T_B_PROLIST_FILE', len(src), ok, matched, '-', len(src)-len(rows2)+fail, sfx))

if __name__ == '__main__':
    etl_old_version_out()
    etl_version_out()
    etl_prolist()
    etl_nobatch()
    etl_attachment()
    print("\n==== 迁移汇总 ====")
    print(f"{'功能':<16}{'源表':<32}{'源数':>6}{'迁入':>6}{'FK':>6}{'快照':>6}{'失败':>6}{'加后缀':>7}")
    tot_src = tot_ok = tot_fail = tot_sfx = 0
    for f, s, src, ok, fk, snap, fail, sfx in report:
        print(f"{f:<16}{s:<32}{src:>6}{ok:>6}{str(fk):>6}{str(snap):>6}{fail:>6}{sfx:>7}")
        tot_src += src; tot_ok += ok; tot_fail += fail; tot_sfx += sfx
    print(f"{'合计':<16}{'':<32}{tot_src:>6}{tot_ok:>6}{'':>6}{'':>6}{tot_fail:>6}{tot_sfx:>7}")
    mysql.close()
    print("done")
