package adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.drinkzlyboozeserver.R;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import callback.IRecyclerClickListener;
import model.ShipperModel;

public class MyShipperSelectionAdapter extends RecyclerView.Adapter<MyShipperSelectionAdapter.MyViewHolder> {
    private Context context;
    List<ShipperModel> shipperModelList;
    private ImageView lastCheckedImageView=null;
    private ShipperModel selectedShipper=null;

    public MyShipperSelectionAdapter(Context context, List<ShipperModel> shipperModelList) {
        this.context = context;
        this.shipperModelList = shipperModelList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.layout_shipper_selected,parent,false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            holder.txt_name_S1.setText(new StringBuilder(shipperModelList.get(position).getName()));
        holder.txt_phone1.setText(new StringBuilder(shipperModelList.get(position).getPhone()));
        holder.setiRecyclerClickListener((view, pos) -> {
            if(lastCheckedImageView!=null)
                lastCheckedImageView.setImageResource(0);
            holder.img_checked.setImageResource(R.drawable.ic_baseline_check_24);
            lastCheckedImageView = holder.img_checked;
            selectedShipper = shipperModelList.get(pos);
        });
    }

    public ShipperModel getSelectedShipper() {
        return selectedShipper;
    }


    @Override
    public int getItemCount() {
        return shipperModelList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            private Unbinder unbinder;

            @BindView(R.id.txt_name_S1)
            TextView txt_name_S1;
            @BindView(R.id.txt_phone1)
            TextView txt_phone1;
            @BindView(R.id.img_checked)
            ImageView img_checked;

            IRecyclerClickListener iRecyclerClickListener;

        public void setiRecyclerClickListener(IRecyclerClickListener iRecyclerClickListener) {
            this.iRecyclerClickListener = iRecyclerClickListener;
        }

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            unbinder = ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            iRecyclerClickListener.onItemClickListener(view,getAdapterPosition());
        }
    }
}
