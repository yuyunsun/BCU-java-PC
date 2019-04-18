package util.unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.InStream;
import io.OutStream;
import main.MainBCU;
import util.Data;
import util.anim.AnimC;
import util.entity.data.CustomUnit;
import util.pack.Pack;
import util.system.FixIndexList;

public class UnitStore extends Data {

	public static Unit get(int uid, boolean print) {
		Pack p = Pack.map.get(uid / 1000);
		if (p != null) {
			Unit u = p.us.ulist.get(uid % 1000);
			if (u != null)
				return u;
			if (print)
				MainBCU.pop("Can't find unit: " + uid, "can't find unit");
			return null;
		}
		if (print)
			MainBCU.pop("can't find pack: " + uid / 1000, "can't find unit");
		return null;
	}

	public static Form get(int uid, int fid, boolean print) {
		Unit u = get(uid, print);
		if (u == null)
			return null;
		if (fid >= u.forms.length) {
			if (print)
				MainBCU.pop("unit " + trio(uid) + " doesn't have form " + fid, "cannot find unit");
			return null;
		}
		return get(uid, print).forms[fid];
	}

	public static Form get(int[] com) {
		return get(com[0], com[1], false);
	}

	public static List<Unit> getAll(Pack p, boolean parent) {
		List<Unit> ans = new ArrayList<>();
		if (p != null) {
			if (parent)
				for (int rel : p.rely)
					ans.addAll(Pack.map.get(rel).us.ulist.getList());
			ans.addAll(p.us.ulist.getList());
		} else
			for (Pack pack : Pack.map.values())
				ans.addAll(pack.us.ulist.getList());
		return ans;
	}

	public static UnitLevel getlevel(int ind) {
		return Pack.map.get(ind / 1000).us.lvlist.get(ind % 1000);
	}

	public final Pack pack;

	public final FixIndexList<UnitLevel> lvlist = new FixIndexList<>(new UnitLevel[1000]);

	public final FixIndexList<Unit> ulist = new FixIndexList<>(new Unit[1000]);

	public UnitStore(Pack p) {
		pack = p;
	}

	public Unit add(DIYAnim da, CustomUnit cu) {
		Unit u = new Unit(pack, da, cu);
		ulist.add(u);
		return u;
	}

	public List<UnitLevel> getlevels() {
		List<UnitLevel> ans = lvlist.getList();
		for (int pid : pack.rely)
			ans.addAll(Pack.map.get(pid).us.lvlist.getList());
		return ans;
	}

	public OutStream packup() {
		OutStream os = new OutStream();
		os.writeString("0.4.1");
		os.writeInt(lvlist.size());
		Map<Integer, UnitLevel> lvmap = lvlist.getMap();
		for (int val : lvmap.keySet()) {
			os.writeInt(val);
			lvmap.get(val).write(os);
		}
		os.writeInt(ulist.size());
		Map<Integer, Unit> umap = ulist.getMap();
		for (int val : umap.keySet()) {
			os.writeInt(val);
			Unit u = umap.get(val);
			os.writeInt(u.lv.id);
			os.writeInt(u.max);
			os.writeInt(u.maxp);
			os.writeInt(u.rarity);
			os.writeInt(u.forms.length);
			for (Form f : u.forms) {
				os.writeString(f.name);
				os.accept(((AnimC) f.anim).write());
				((CustomUnit) f.du).write(os);
			}
		}

		// TODO unit reserved 2
		os.writeInt(0);
		// TODO unit reserved 3
		os.writeInt(0);
		os.terminate();
		return os;
	}

	public OutStream write() {
		OutStream os = new OutStream();
		os.writeString("0.4.1");
		os.writeInt(lvlist.size());
		Map<Integer, UnitLevel> lvmap = lvlist.getMap();
		for (int val : lvmap.keySet()) {
			os.writeInt(val);
			lvmap.get(val).write(os);
		}
		os.writeInt(ulist.size());
		Map<Integer, Unit> umap = ulist.getMap();
		for (int val : umap.keySet()) {
			os.writeInt(val);
			Unit u = umap.get(val);
			os.writeInt(u.lv.id);
			os.writeInt(u.max);
			os.writeInt(u.maxp);
			os.writeInt(u.rarity);
			os.writeInt(u.forms.length);
			for (Form f : u.forms) {
				os.writeString(f.name);
				os.accept(DIYAnim.writeAnim((AnimC) f.anim));
				((CustomUnit) f.du).write(os);
			}
		}

		// TODO unit reserved 2
		os.writeInt(0);
		// TODO unit reserved 3
		os.writeInt(0);

		os.terminate();
		return os;
	}

	public void zreadp(InStream is) {
		int val = getVer(is.nextString());
		if (val >= 401)
			zreadp$000401(val, is);
	}

	public void zreadt(InStream is) {
		int val = getVer(is.nextString());
		if (val >= 401)
			zreadt$000401(val, is);
		else if (val >= 0)
			zreadt$000000(val, is);
	}

	private void zreadp$000401(int ver, InStream is) {
		int n = is.nextInt();
		for (int i = 0; i < n; i++) {
			int ind = is.nextInt();
			UnitLevel ul = new UnitLevel(pack, ind, is);
			lvlist.set(ind, ul);
		}
		n = is.nextInt();
		for (int i = 0; i < n; i++) {
			int ind = is.nextInt();
			Unit u = new Unit(pack, pack.id * 1000 + ind);
			u.lv = getlevel(is.nextInt());
			u.lv.units.add(u);
			u.max = is.nextInt();
			u.maxp = is.nextInt();
			u.rarity = is.nextInt();
			int m = is.nextInt();
			u.forms = new Form[m];
			for (int j = 0; j < m; j++) {
				String name = is.nextString();
				AnimC ac = new AnimC(is.subStream());
				CustomUnit cu = new CustomUnit();
				cu.fillData(ver, is);
				u.forms[j] = new Form(u, j, name, ac, cu);
			}
			ulist.set(ind, u);
		}
	}

	private void zreadt$000000(int ver, InStream is) {
		int n = is.nextInt();
		for (int i = 0; i < n; i++) {
			int ind = is.nextInt();
			UnitLevel ul = new UnitLevel(pack, ind, is);
			lvlist.set(ind, ul);
		}
		n = is.nextInt();
		for (int i = 0; i < n; i++) {
			int ind = is.nextInt();
			Unit u = new Unit(pack, pack.id * 1000 + ind);
			u.lv = getlevel(is.nextInt());
			u.lv.units.add(u);
			u.max = is.nextInt();
			u.maxp = is.nextInt();
			u.rarity = is.nextInt();
			int m = is.nextInt();
			u.forms = new Form[m];
			for (int j = 0; j < m; j++) {
				String name = is.nextString();
				AnimC ac = DIYAnim.getAnim(is.nextString(), false);
				CustomUnit cu = new CustomUnit();
				cu.fillData(ver, is);
				u.forms[j] = new Form(u, j, name, ac, cu);
			}
			ulist.set(ind, u);
		}
	}

	private void zreadt$000401(int ver, InStream is) {
		int n = is.nextInt();
		for (int i = 0; i < n; i++) {
			int ind = is.nextInt();
			UnitLevel ul = new UnitLevel(pack, ind, is);
			lvlist.set(ind, ul);
		}
		n = is.nextInt();
		for (int i = 0; i < n; i++) {
			int ind = is.nextInt();
			Unit u = new Unit(pack, pack.id * 1000 + ind);
			u.lv = getlevel(is.nextInt());
			u.lv.units.add(u);
			u.max = is.nextInt();
			u.maxp = is.nextInt();
			u.rarity = is.nextInt();
			int m = is.nextInt();
			u.forms = new Form[m];
			for (int j = 0; j < m; j++) {
				String name = is.nextString();
				AnimC ac = DIYAnim.zread(is.subStream(), false);
				CustomUnit cu = new CustomUnit();
				cu.fillData(ver, is);
				u.forms[j] = new Form(u, j, name, ac, cu);
			}
			ulist.set(ind, u);
		}
	}

}