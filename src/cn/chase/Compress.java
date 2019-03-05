package cn.chase;

import java.io.*;

import static cn.chase.BitFile.*;

public class Compress {
    public static int min_rep_len = 11;
    public static int max_arr_num_bit = 2 * min_rep_len;
    public static int max_arr_num = 1 << max_arr_num_bit;
    public static int MAX_CHAR_NUM = 1 << 26;
    public static int min_size = 1 << 20;

    public static String identifier;
    public static int ref_low_vec_len = 0, tar_low_vec_len = 0, line_break_len = 0, other_char_len = 0, N_vec_len = 0, line_len = 0, ref_seq_len = 0, tar_seq_len = 0;
    public static int diff_pos_loc_len, diff_low_vec_len = 0;

    public static char[] ref_seq_code = new char[MAX_CHAR_NUM];
    public static char[] tar_seq_code = new char[MAX_CHAR_NUM];
    public static int[] ref_low_vec_begin = new int[min_size];
    public static int[] ref_low_vec_length = new int[min_size];
    public static int[] tar_low_vec_begin = new int[min_size];
    public static int[] tar_low_vec_length = new int[min_size];
    public static int[] N_vec_begin = new int[min_size];
    public static int[] N_vec_length = new int[min_size];
    public static int[] other_char_vec_pos = new int[min_size];
    public static char[] other_char_vec_ch = new char[min_size];
    public static int[] diff_low_vec_begin = new int[min_size];
    public static int[] diff_low_vec_length = new int[min_size];
    public static int[] line_start = new int[min_size];
    public static int[] line_length = new int[min_size];
    public static int[] diff_pos_loc_begin = new int[min_size];
    public static int[] diff_pos_loc_length = new int[min_size];
    public static int[] line_break_vec = new int[1 << 25];
    public static int[] point = new int[max_arr_num];
    public static int[] loc = new int[MAX_CHAR_NUM];
    public static int[] diff_low_loc = new int[min_size];
    public static char[] dismatched_str = new char[min_size];

    public static byte agctIndex(char ch) {
        if (ch == 'A') {
            return 0;
        }
        if (ch == 'C') {
            return 1;
        }
        if (ch == 'G') {
            return 2;
        }
        if (ch == 'T') {
            return 3;
        }
        return -1;
    }

    public static void extractRefInfo(File refFile) {
        BufferedReader br = null;
        String str;
        int str_length;
        char ch;
        Boolean flag = true;
        int letters_len = 0;

        try {
            br = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(refFile))));
            br.readLine();
            while ((str = br.readLine()) != null) {
                str_length = str.length();
                for (int i = 0; i < str_length; i++) {
                    ch = str.charAt(i);

                    if (Character.isLowerCase(ch)) {
                        ch = Character.toUpperCase(ch);

                        if (flag) {
                            flag = false;
                            ref_low_vec_begin[ref_low_vec_len] = letters_len;
                            letters_len = 0;
                        }
                    } else {
                        if (!flag) {
                            flag = true;
                            ref_low_vec_length[ref_low_vec_len++] = letters_len;
                            letters_len = 0;
                        }
                    }

                    if (ch == 'A' || ch == 'C' || ch == 'G' || ch == 'T') {
                        ref_seq_code[ref_seq_len ++] = ch;
                    }

                    letters_len ++;
                }
            }

            if (!flag) {
                ref_low_vec_length[ref_low_vec_len ++] = letters_len;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void extractTarInfo(File tarFile) {
        BufferedReader br = null;
        String str;
        char ch;
        int str_length;
        boolean flag = true, n_flag = false;
        int letters_len = 0, n_letters_len = 0;

        try {
            br = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(tarFile))));
            identifier = br.readLine();   //先读取一行基因文件标识

            while ((str = br.readLine()) != null) {
                str_length = str.length();
                for (int i = 0; i < str_length; i++) {
                    ch = str.charAt(i);

                    if (Character.isLowerCase(ch)) {
                        ch = Character.toUpperCase(ch);

                        if (flag) {
                            flag = false;
                            tar_low_vec_begin[tar_low_vec_len] = letters_len;
                            letters_len = 0;
                        }
                    } else {
                        if (!flag) {
                            flag = true;
                            tar_low_vec_length[tar_low_vec_len ++] = letters_len;
                            letters_len = 0;
                        }
                    }
                    letters_len ++;

                    if(ch == 'A'||ch == 'G'||ch == 'C'||ch == 'T') {
                        tar_seq_code[tar_seq_len ++] = ch;
                    } else if(ch != 'N') {
                        other_char_vec_pos[other_char_len] = tar_seq_len;
                        other_char_vec_ch[other_char_len ++] = ch;
                    }

                    if (!n_flag) {
                        if (ch == 'N') {
                            N_vec_begin[N_vec_len] = n_letters_len;
                            n_letters_len = 0;
                            n_flag = true;
                        }
                    } else {
                        if (ch != 'N'){
                            N_vec_length[N_vec_len ++] = n_letters_len;
                            n_letters_len = 0;
                            n_flag = false;
                        }
                    }
                    n_letters_len++;
                }

                line_break_vec[line_break_len ++] = str_length;
            }

            if (!flag) {
                tar_low_vec_length[tar_low_vec_len++] = letters_len;
            }

            if (n_flag) {
                N_vec_length[N_vec_len++] = n_letters_len;
            }

            for (int i = other_char_len - 1; i > 0; i --) {
                other_char_vec_pos[i] -= other_char_vec_pos[i - 1];
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (line_break_len > 0) {
            int cnt = 1;
            line_start[line_len] = line_break_vec[0];
            for (int i = 1; i < line_break_len; i ++) {
                if (line_start[line_len] == line_break_vec[i]) {
                    cnt ++;
                } else {
                    line_length[line_len ++] = cnt;
                    line_start[line_len] = line_break_vec[i];
                    cnt = 1;
                }
            }
            line_length[line_len ++] = cnt;
        }
    }

    public static void kMerHashingConstruct() {
        int value = 0;
        int step_len = ref_seq_len - min_rep_len + 1;
        for (int i = 0; i < max_arr_num; i ++) {
            point[i] = -1;
        }

        for (int k = min_rep_len - 1; k >= 0; k --) {
            value <<= 2;
            value += agctIndex(ref_seq_code[k]);
        }
        loc[0] = point[value];
        point[value] = 0;

        int shift_bit_num = (min_rep_len * 2 - 2);
        int one_sub_str = min_rep_len - 1;
        for (int i = 1; i < step_len; i ++) {
            value >>= 2;
            value += (agctIndex(ref_seq_code[i + one_sub_str])) << shift_bit_num;
            loc[i] = point[value];
            point[value] = i;
        }
    }

    public static void searchMatchPosVec() {    //二次压缩小写字符二元组
        for (int x = 0; x < tar_low_vec_len; x ++) {
            diff_low_loc[x] = 0;
        }

//        for(int i = 0; i < tar_low_vec_len; i ++){
//            //search from the start_position to the end
//            for (int j = start_position; j < ref_low_vec_len; j ++){
//                if ((tar_low_vec_begin[i] == ref_low_vec_begin[j]) && (tar_low_vec_length[i] == ref_low_vec_length[j])) {
//                    diff_low_loc[i] = j;
//                    start_position = j + 1;
//                    break;
//                }
//            }
//
//            //search from the start_position to the begin
//            if(diff_low_loc[i] == 0) {
//                for (int j = start_position - 1; j > 0; j --){
//                    if ((tar_low_vec_begin[i] == ref_low_vec_begin[j]) && (tar_low_vec_length[i] == ref_low_vec_length[j])){
//                        diff_low_loc[i] = j;
//                        start_position = j + 1;
//                        break;
//                    }
//                }
//            }
//
//            //record the mismatched information
//            if(diff_low_loc[i] == 0){
//                diff_low_vec_begin[diff_low_vec_len] = tar_low_vec_begin[i];
//                diff_low_vec_length[diff_low_vec_len ++] = tar_low_vec_length[i ++];
//            }
//        }

        int start_position = 0, i = 0;
        out:
        while(i < tar_low_vec_len) {
            for (int j = start_position; j < ref_low_vec_len; j ++) {
                if ((tar_low_vec_begin[i] == ref_low_vec_begin[j]) && (tar_low_vec_length[i] == ref_low_vec_length[j])) {
                    diff_low_loc[i] = j;
                    start_position = j + 1;
                    i ++;
                    continue out;
                }
            }
            for (int j = start_position - 1; j > 0; j --) {
                if ((tar_low_vec_begin[i] == ref_low_vec_begin[j]) && (tar_low_vec_length[i] == ref_low_vec_length[j])) {
                    diff_low_loc[i] = j;
                    start_position = j + 1;
                    i ++;
                    continue out;
                }
            }
            diff_low_vec_begin[diff_low_vec_len] = tar_low_vec_begin[i];
            diff_low_vec_length[diff_low_vec_len ++] = tar_low_vec_length[i ++];
        }

        //diff_low_loc[i]可能是连续的数字，再次压缩成二元组
        if (tar_low_vec_len > 0) {
            int cnt = 1;
            diff_pos_loc_begin[diff_pos_loc_len] = diff_low_loc[0];
            for (int x = 1; x < tar_low_vec_len; x ++) {
                if ((diff_low_loc[x] - diff_low_loc[x - 1]) == 1) {
                    cnt ++;
                } else {
                    diff_pos_loc_length[diff_pos_loc_len ++] = cnt;
                    diff_pos_loc_begin[diff_pos_loc_len] = diff_low_loc[x];
                    cnt = 1;
                }
            }
            diff_pos_loc_length[diff_pos_loc_len ++] = cnt;
        }
    }

    public static void binaryCoding(Stream stream, int num) {
        int type;

        if (num > MAX_CHAR_NUM) {
            System.out.println("Too large to Write!\n");
            return;
        }

        if (num < 2) {
            type = 1;
            bitFilePutBitsInt(stream, type, 2);
            bitFilePutBit(stream, num);
        } else if (num < 262146) {
            type = 1;
            num -= 2;
            bitFilePutBit(stream, type);
            bitFilePutBitsInt(stream, num, 18);
        } else {
            type = 0;
            num -= 262146;
            bitFilePutBitsInt(stream, type, 2);
            bitFilePutBitsInt(stream, num, 28);
        }
    }

    public static void saveOtherData(Stream stream) {
        binaryCoding(stream, identifier.length());
        char[] meta_data = identifier.toCharArray();
        for(int i = 0; i < identifier.length(); i ++) {
            bitFilePutChar(stream, meta_data[i]);
        }

        binaryCoding(stream, line_len);
        for (int i = 0; i < line_len; i ++) {
            binaryCoding(stream, line_start[i]);
            binaryCoding(stream, line_length[i]);
        }

        binaryCoding(stream, diff_pos_loc_len);
        for (int i = 0; i < diff_pos_loc_len; i ++) {
            binaryCoding(stream, diff_pos_loc_begin[i]);
            binaryCoding(stream, diff_pos_loc_length[i]);
        }

        binaryCoding(stream, diff_low_vec_len);
        for (int i = 0; i < diff_low_vec_len; i ++) {
            binaryCoding(stream, diff_low_vec_begin[i]);
            binaryCoding(stream, diff_low_vec_length[i]);
        }

        binaryCoding(stream, N_vec_len);
        for (int i = 0; i < N_vec_len; i ++) {
            binaryCoding(stream, N_vec_begin[i]);
            binaryCoding(stream, N_vec_length[i]);
        }

        binaryCoding(stream, other_char_len);
        if (other_char_len > 0) {
            for(int i = 0; i < other_char_len; i ++){
                binaryCoding(stream, other_char_vec_pos[i]);
                bitFilePutChar(stream, other_char_vec_ch[i] - 'A');
            }
        }
    }

    public static void searchMatchSeqCode(Stream stream) {
        int pre_pos = 0;
        int step_len = tar_seq_len - min_rep_len + 1;
        int max_length, max_k;

        int dis_str_len = 0, i, id, k, ref_idx, tar_idx, length, cur_pos;
        int tar_value;

        for (i = 0; i < step_len; i++) {
            tar_value = 0;
            for (k = min_rep_len - 1; k >= 0; k--) {
                tar_value <<= 2;
                tar_value += agctIndex(tar_seq_code[i + k]);
            }

            id = point[tar_value];
            if (id > -1) {
                max_length = -1;
                max_k = -1;
                for (k = id; k != -1; k = loc[k]) {
                    ref_idx = k + min_rep_len;
                    tar_idx = i + min_rep_len;
                    length = min_rep_len;
                    while (ref_idx < ref_seq_len && tar_idx < tar_seq_len && ref_seq_code[ref_idx ++] == tar_seq_code[tar_idx ++]) {
                        length++;
                    }
                    if (length > max_length) {
                        max_length = length;
                        max_k = k;
                    }
                }
                binaryCoding(stream, dis_str_len);
                if (dis_str_len > 0) {
                    for(int x = 0; x < dis_str_len; x ++)
                        bitFilePutChar(stream, dismatched_str[x] - 'A');
                    dis_str_len = 0;
                }
                cur_pos = max_k - pre_pos;
                pre_pos = max_k + max_length;
                if(cur_pos < 0){
                    cur_pos = -cur_pos;
                    int type = 1;
                    bitFilePutBit(stream, type);
                    binaryCoding(stream, cur_pos);
                    binaryCoding(stream,max_length - min_rep_len);
                }
                else{
                    int type=0;
                    bitFilePutBit(stream, type);
                    binaryCoding(stream, cur_pos);
                    binaryCoding(stream,max_length - min_rep_len);
                }

                i += max_length-1;
                continue;
            }
            dismatched_str[dis_str_len ++] = tar_seq_code[i];
        }

        for(; i < tar_seq_len; i++) {
            dismatched_str[dis_str_len ++] = tar_seq_code[i];
        }
        binaryCoding(stream, dis_str_len);
        if (dis_str_len > 0) {
            for(int x = 0; x < dis_str_len; x ++) {
                bitFilePutChar(stream, dismatched_str[x] - 'A');
            }
        }
    }

    public static void main(String[] args) {
        File refFile = new File("C:/Users/chase/OneDrive/GeneFiles/hg17_chr21.fa");
        File tarFile = new File("C:/Users/chase/OneDrive/GeneFiles/hg18_chr21.fa");
        File resultFile = new File("E:/result.txt");
        Stream stream = new Stream(resultFile, 0, 0);

        long startTime = System.currentTimeMillis();
        long time = System.currentTimeMillis();
        extractRefInfo(refFile);
        System.out.println("readRefFile耗费时间为" + (System.currentTimeMillis() - time) + "ms");
        time = System.currentTimeMillis();

        kMerHashingConstruct();
        System.out.println("preProcessRef耗费时间为" + (System.currentTimeMillis() - time) + "ms");
        time = System.currentTimeMillis();

        extractTarInfo(tarFile);
        System.out.println("readTarFile耗费时间为" + (System.currentTimeMillis() - time) + "ms");
        time = System.currentTimeMillis();

        searchMatchPosVec();
        System.out.println("searchMatchPosVec耗费时间为" + (System.currentTimeMillis() - time) + "ms");
        time = System.currentTimeMillis();

        saveOtherData(stream);
        System.out.println("saveOtherData耗费时间为" + (System.currentTimeMillis() - time) + "ms");
        time = System.currentTimeMillis();

        searchMatchSeqCode(stream);
        System.out.println("serarchMatch耗费时间为" + (System.currentTimeMillis() - time) + "ms");
        System.out.println("总耗费时间为" + (System.currentTimeMillis() - startTime) + "ms");
    }
}
